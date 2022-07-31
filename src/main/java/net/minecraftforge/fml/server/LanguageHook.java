/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mohistmc.util.i18n.i18n;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.LanguageMap;
import net.minecraftforge.fml.ForgeI18n;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class LanguageHook
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Pattern PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static List<Map<String, String>> capturedTables = new ArrayList<>(2);
    private static Map<String, String> modTable;
    /**
     * Loads lang files on the server
     */
    public static void captureLanguageMap(Map<String, String> table)
    {
        capturedTables.add(table);
        if (modTable != null) {
            capturedTables.forEach(t->t.putAll(modTable));
        }
    }

    // The below is based on client side net.minecraft.client.resources.Locale code
    private static void loadLocaleData(final List<IResource> allResources) {
        allResources.stream().map(IResource::getInputStream).forEach(LanguageHook::loadLocaleData);
    }

    private static void loadLocaleData(final InputStream inputstream) {
        try
        {
            JsonElement jsonelement = GSON.fromJson(new InputStreamReader(inputstream, StandardCharsets.UTF_8), JsonElement.class);
            JsonObject jsonobject = JSONUtils.convertToJsonObject(jsonelement, "strings");

            jsonobject.entrySet().forEach(entry -> {
                String s = PATTERN.matcher(JSONUtils.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
                modTable.put(entry.getKey(), s);
            });
        }
        finally
        {
            IOUtils.closeQuietly(inputstream);
        }
    }

    private static void loadLanguage(String langName, MinecraftServer server) {
        String langFile = String.format("lang/%s.json", langName);
        IResourceManager resourceManager = server.getDataPackRegistries().getResourceManager();
        resourceManager.getNamespaces().forEach(namespace -> {
            try {
                ResourceLocation langResource = new ResourceLocation(namespace, langFile);
                loadLocaleData(resourceManager.getResources(langResource));
            } catch (FileNotFoundException fnfe) {
            } catch (Exception exception) {
                LOGGER.warn("Skipped language file: {}:{}", namespace, langFile, exception);
            }
        });

    }

    public static void loadForgeAndMCLangs() {
        modTable = new HashMap<>(5000);
        final InputStream mc = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/minecraft/lang/en_us.json");
        final InputStream forge = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/forge/lang/en_us.json");
        loadLocaleData(mc);
        loadLocaleData(forge);
        capturedTables.forEach(t->t.putAll(modTable));
        ForgeI18n.loadLanguageData(modTable);
    }

    static void loadLanguagesOnServer(MinecraftServer server) {
        modTable = new HashMap<>(5000);
        // Possible multi-language server support?
        for (String lang : Arrays.asList(i18n.getVanillaLanguage())) {
            loadLanguage(lang, server);
        }
        capturedTables.forEach(t->t.putAll(modTable));
        ForgeI18n.loadLanguageData(modTable);
    }
}
