/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.server;

import static net.minecraftforge.fml.Logging.CORE;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.mohistmc.util.i18n.i18n;
import net.minecraft.resources.IPackNameDecorator;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.network.ConnectionType;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.FMLStatusPing;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.fml.packs.ResourcePackLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.GameData;
import org.spigotmc.SpigotConfig;

public class ServerLifecycleHooks
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker SERVERHOOKS = MarkerManager.getMarker("SERVERHOOKS");
    private static final FolderName SERVERCONFIG = new FolderName("serverconfig");
    private static volatile CountDownLatch exitLatch = null;
    private static MinecraftServer currentServer;

    private static Path getServerConfigPath(final MinecraftServer server)
    {
        final Path serverConfig = server.getWorldPath(SERVERCONFIG);
        FileUtils.getOrCreateDirectory(serverConfig, "serverconfig");
        return serverConfig;
    }

    public static boolean handleServerAboutToStart(final MinecraftServer server)
    {
        currentServer = server;
        currentServer.getStatus().setForgeData(new FMLStatusPing()); //gathers NetworkRegistry data
        // on the dedi server we need to force the stuff to setup properly
        LogicalSidedProvider.setServer(()->server);
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, getServerConfigPath(server));
        return !MinecraftForge.EVENT_BUS.post(new FMLServerAboutToStartEvent(server));
    }

    public static boolean handleServerStarting(final MinecraftServer server)
    {
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, ()->()->LanguageHook.loadLanguagesOnServer(server));
        return !MinecraftForge.EVENT_BUS.post(new FMLServerStartingEvent(server));
    }

    public static void handleServerStarted(final MinecraftServer server)
    {
        MinecraftForge.EVENT_BUS.post(new FMLServerStartedEvent(server));
        allowLogins.set(true);
    }

    public static void handleServerStopping(final MinecraftServer server)
    {
        allowLogins.set(false);
        MinecraftForge.EVENT_BUS.post(new FMLServerStoppingEvent(server));
    }

    public static void expectServerStopped()
    {
        exitLatch = new CountDownLatch(1);
    }

    public static void handleServerStopped(final MinecraftServer server)
    {
        if (!server.isDedicatedServer()) GameData.revertToFrozen();
        MinecraftForge.EVENT_BUS.post(new FMLServerStoppedEvent(server));
        currentServer = null;
        LogicalSidedProvider.setServer(null);
        CountDownLatch latch = exitLatch;

        if (latch != null)
        {
            latch.countDown();
            exitLatch = null;
        }
        ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER, getServerConfigPath(server));
    }

    public static MinecraftServer getCurrentServer()
    {
        return currentServer;
    }
    private static AtomicBoolean allowLogins = new AtomicBoolean(false);

    public static boolean handleServerLogin(final CHandshakePacket packet, final NetworkManager manager) {
        if (!allowLogins.get())
        {
            StringTextComponent text = new StringTextComponent(i18n.get("serverlifecyclehooks.1"));
            LOGGER.info(SERVERHOOKS, i18n.get("serverlifecyclehooks.2", text.getContents()));
            manager.send(new SDisconnectLoginPacket(text));
            manager.disconnect(text);
            return false;
        }

        if (packet.getIntention() == ProtocolType.LOGIN) {
            final ConnectionType connectionType = ConnectionType.forVersionFlag(packet.getFMLVersion());
            final int versionNumber = connectionType.getFMLVersionNumber(packet.getFMLVersion());

            if (connectionType == ConnectionType.MODDED && versionNumber != FMLNetworkConstants.FMLNETVERSION) {
                rejectConnection(manager, connectionType, i18n.get("serverlifecyclehooks.3", versionNumber, FMLNetworkConstants.FMLNETVERSION));
                return false;
            }

            if (!SpigotConfig.bungee && connectionType == ConnectionType.VANILLA && !NetworkRegistry.acceptsVanillaClientConnections()) {
                rejectConnection(manager, connectionType, i18n.get("serverlifecyclehooks.4"));
                return false;
            }
        }

        if (packet.getIntention() == ProtocolType.STATUS) return true;

        NetworkHooks.registerServerLoginChannel(manager, packet);
        return true;

    }

    private static void rejectConnection(final NetworkManager manager, ConnectionType type, String message) {
        manager.setProtocol(ProtocolType.LOGIN);
        LOGGER.info(SERVERHOOKS, i18n.get("serverlifecyclehooks.5", type, message));
        StringTextComponent text = new StringTextComponent(message);
        manager.send(new SDisconnectLoginPacket(text));
        manager.disconnect(text);
    }

    public static void handleExit(int retVal)
    {
        System.exit(retVal);
    }

    //INTERNAL MODDERS DO NOT USE
    @Deprecated
    public static ResourcePackLoader.IPackInfoFinder buildPackFinder(Map<ModFile, ? extends ModFileResourcePack> modResourcePacks, BiConsumer<? super ModFileResourcePack, ResourcePackInfo> packSetter) {
        return (packList, factory) -> serverPackFinder(modResourcePacks, packSetter, packList, factory);
    }

    private static void serverPackFinder(Map<ModFile, ? extends ModFileResourcePack> modResourcePacks, BiConsumer<? super ModFileResourcePack, ResourcePackInfo> packSetter, Consumer<ResourcePackInfo> consumer, ResourcePackInfo.IFactory factory) {
        for (Entry<ModFile, ? extends ModFileResourcePack> e : modResourcePacks.entrySet())
        {
            IModInfo mod = e.getKey().getModInfos().get(0);
            if (Objects.equals(mod.getModId(), "minecraft")) continue; // skip the minecraft "mod"
            final String name = "mod:" + mod.getModId();
            final ResourcePackInfo packInfo = ResourcePackInfo.create(name, false, e::getValue, factory, ResourcePackInfo.Priority.BOTTOM, IPackNameDecorator.DEFAULT);
            if (packInfo == null) {
                // Vanilla only logs an error, instead of propagating, so handle null and warn that something went wrong
                ModLoader.get().addWarning(new ModLoadingWarning(mod, ModLoadingStage.ERROR, "fml.modloading.brokenresources", e.getKey()));
                continue;
            }
            packSetter.accept(e.getValue(), packInfo);
            LOGGER.debug(CORE, i18n.get("serverlifecyclehooks.6", name, e.getKey().getFilePath()));
            consumer.accept(packInfo);
        }
    }
}