/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.furnace;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * {@link FurnaceFuelBurnTimeEvent} is fired when determining the fuel value for an ItemStack. <br>
 * <br>
 * To set the burn time of your own item, use {@link Item#getBurnTime(ItemStack)} instead.<br>
 * <br>
 * This event is fired from {@link ForgeEventFactory#getItemBurnTime(ItemStack)}.<br>
 * <br>
 * This event is {@link Cancelable} to prevent later handlers from changing the value.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 **/
@Cancelable
public class FurnaceFuelBurnTimeEvent extends Event
{
    @Nonnull
    private final ItemStack itemStack;
    @Nullable
    private final IRecipeType<?> recipeType;
    private int burnTime;

    @Deprecated // use version with IRecipeType
    public FurnaceFuelBurnTimeEvent(@Nonnull ItemStack itemStack, int burnTime)
    {
        this(itemStack, burnTime, null);
    }

    public FurnaceFuelBurnTimeEvent(@Nonnull ItemStack itemStack, int burnTime, @Nullable IRecipeType<?> recipeType)
    {
        this.itemStack = itemStack;
        this.burnTime = burnTime;
        this.recipeType = recipeType;
    }

    /**
     * Get the ItemStack "fuel" in question.
     */
    @Nonnull
    public ItemStack getItemStack()
    {
        return itemStack;
    }

    /**
     *
     * Get the recipe type for which to obtain the burn time, if known.
     */
    @Nullable
    public IRecipeType<?> getRecipeType()
    {
        return recipeType;
    }

    /**
     * Set the burn time for the given ItemStack.
     * Setting it to 0 will prevent the item from being used as fuel, overriding vanilla's decision.
     */
    public void setBurnTime(int burnTime)
    {
        if (burnTime >= 0)
        {
            this.burnTime = burnTime;
            setCanceled(true);
        }
    }

    /**
     * The resulting value of this event, the burn time for the ItemStack.
     * A value of 0 will prevent the item from being used as fuel, overriding vanilla's decision.
     */
    public int getBurnTime()
    {
        return burnTime;
    }
}
