package dev.kyuba.region_rtp.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a delayed teleport: the player must stand still for a configurable
 * number of seconds. If the player moves more than one block or takes damage
 * before the delay elapses, the teleport is cancelled.
 *
 * <p>For an instant teleport (delay 0) the caller should teleport directly
 * and not use this class.
 */
public final class TeleportTask implements Listener {

    private final Plugin plugin;
    private final Player player;
    private final Location destination;
    private final int delaySeconds;
    private final Runnable onSuccess;
    private final Runnable onCancel;
    private final Location startLocation;
    private BukkitTask task;

    /**
     * Start a delayed teleport.
     *
     * @param plugin       the owning plugin
     * @param player       the player to teleport
     * @param destination  the destination location
     * @param delaySeconds seconds the player must stand still (must be >= 1)
     * @param onSuccess    called on the main thread when the teleport is applied
     * @param onCancel     called on the main thread when the teleport is cancelled
     */
    public TeleportTask(@NotNull Plugin plugin, @NotNull Player player,
                        @NotNull Location destination, int delaySeconds,
                        @NotNull Runnable onSuccess, @NotNull Runnable onCancel) {
        this.plugin = plugin;
        this.player = player;
        this.destination = destination;
        this.delaySeconds = delaySeconds;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
        this.startLocation = player.getLocation().clone();
    }

    /**
     * Begin the countdown and register the cancellation listener.
     */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            complete();
        }, delaySeconds * 20L);
    }

    /**
     * Apply the teleport and clean up.
     */
    private void complete() {
        HandlerList.unregisterAll(this);
        if (task != null) {
            task.cancel();
            task = null;
        }
        player.teleport(destination);
        onSuccess.run();
    }

    /**
     * Cancel the pending teleport and clean up.
     *
     * @param reason message shown to the player
     */
    public void cancel(@NotNull String reason) {
        if (task == null) {
            return; // already completed or cancelled
        }
        task.cancel();
        task = null;
        HandlerList.unregisterAll(this);
        player.sendMessage(reason);
        onCancel.run();
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.getPlayer().equals(player)) {
            return;
        }
        // Only cancel if the player actually changed block position (not just head rotation)
        Location to = event.getTo();
        if (to.getBlockX() != startLocation.getBlockX()
                || to.getBlockY() != startLocation.getBlockY()
                || to.getBlockZ() != startLocation.getBlockZ()) {
            cancel("§cTeleport cancelled — you moved.");
        }
    }

    @EventHandler
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!event.getEntity().equals(player)) {
            return;
        }
        cancel("§cTeleport cancelled — you took damage.");
    }
}