package org.bukkit.event.player;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a player attempts to pick an item up from the ground
 */
public class PlayerAttemptPickupItemEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @NotNull private final Item item;
    private final int remaining;
    private boolean flyAtPlayer = true;
    private boolean isCancelled = false;

    @Deprecated // Remove in 1.13 // Remove in 1.14?
    public PlayerAttemptPickupItemEvent(@NotNull final Player player, @NotNull final Item item) {
        this(player, item, 0);
    }

    public PlayerAttemptPickupItemEvent(@NotNull final Player player, @NotNull final Item item, final int remaining) {
        super(player);
        this.item = item;
        this.remaining = remaining;
    }

    /**
     * Gets the Item attempted by the player.
     *
     * @return Item
     */
    @NotNull
    public Item getItem() {
        return item;
    }

    /**
     * Gets the amount that will remain on the ground, if any
     *
     * @return amount that will remain on the ground
     */
    public int getRemaining() {
        return remaining;
    }

    /**
     * Set if the item will fly at the player
     * <p>Cancelling the event will set this value to false.</p>
     *
     * @param flyAtPlayer True for item to fly at player
     */
    public void setFlyAtPlayer(boolean flyAtPlayer) {
        this.flyAtPlayer = flyAtPlayer;
    }

    /**
     * Gets if the item will fly at the player
     *
     * @return True if the item will fly at the player
     */
    public boolean getFlyAtPlayer() {
        return this.flyAtPlayer;
    }


    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
        this.flyAtPlayer = !cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
