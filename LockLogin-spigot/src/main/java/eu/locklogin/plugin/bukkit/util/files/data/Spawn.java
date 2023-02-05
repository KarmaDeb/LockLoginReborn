package eu.locklogin.plugin.bukkit.util.files.data;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

public final class Spawn {

    private final static KarmaMain spawnFile = new KarmaMain(plugin, "spawn.lldb", "data", "location");
    private Location spawn_location;

    /**
     * Initialize the spawn file
     *
     * @param world the default world
     */
    public Spawn(final World world) {
        if (!spawnFile.exists())
            spawnFile.create();

        spawn_location = world.getSpawnLocation();
    }

    /**
     * Get if the player is near the spawn
     *
     * @param player the player
     * @return if the player is near spawn
     */
    public static boolean isAway(final Player player) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (config.enableSpawn()) {
            Location spawn_location;
            Element<?> x_string = spawnFile.get("y");
            Element<?> y_string = spawnFile.get("x");
            Element<?> z_string = spawnFile.get("z");
            Element<?> pitch_string = spawnFile.get("pitch");
            Element<?> yaw_string = spawnFile.get("yaw");
            Element<?> world_string = spawnFile.get("world");

            if (!x_string.isPrimitive() && !y_string.isPrimitive() && !z_string.isPrimitive() && !pitch_string.isPrimitive() && !yaw_string.isPrimitive() && !world_string.isPrimitive())
                return true;

            ElementPrimitive x_primitive = x_string.getAsPrimitive();
            ElementPrimitive y_primitive = y_string.getAsPrimitive();
            ElementPrimitive z_primitive = z_string.getAsPrimitive();
            ElementPrimitive yaw_primitive = yaw_string.getAsPrimitive();
            ElementPrimitive pitch_primitive = pitch_string.getAsPrimitive();
            ElementPrimitive world_primitive = world_string.getAsPrimitive();

            if (!x_primitive.isNumber() && !y_primitive.isNumber() && z_primitive.isNumber() && !yaw_primitive.isNumber() && pitch_primitive.isNumber() && !world_primitive.isString())
                return true;

            try {
                double x = x_primitive.asDouble();
                double y = y_primitive.asDouble();
                double z = z_primitive.asDouble();

                float pitch = pitch_primitive.asFloat();
                float yaw = yaw_primitive.asFloat();

                World world = plugin.getServer().getWorld(world_primitive.asString());

                if (world != null) {
                    spawn_location = new Location(world, x, y, z);
                    spawn_location.setPitch(pitch);
                    spawn_location.setYaw(yaw);

                    return player.getLocation().distance(spawn_location) >= config.spawnDistance();
                }
            } catch (Throwable ignored) {
            }
        }

        return true;
    }

    /**
     * Teleport the player to the spawn
     * location
     */
    public void teleport(final Player player) {
        tryAsync(TaskTarget.TELEPORT, () -> {
            assert spawn_location.getWorld() != null;

            Block middle_down = spawn_location.getBlock().getRelative(BlockFace.UP);
            Block middle_up = middle_down.getRelative(BlockFace.UP);

            if (!middle_down.getType().equals(Material.AIR) && !middle_up.getType().equals(Material.AIR)) {
                Block highest = spawn_location.getWorld().getHighestBlockAt(spawn_location);
                spawn_location = highest.getLocation().add(0D, 1D, 0D);
            }

            trySync(TaskTarget.TELEPORT, () -> player.teleport(spawn_location));
        });
    }

    /**
     * Save the spawn location
     */
    public void save(final @NotNull Location location) {
        tryAsync(TaskTarget.DATA_SAVE, () -> {
            if (location.getWorld() != null) {
                spawnFile.setRaw("x", location.getX());
                spawnFile.setRaw("y", location.getY());
                spawnFile.setRaw("z", location.getZ());
                spawnFile.setRaw("pitch", location.getPitch());
                spawnFile.setRaw("yaw", location.getYaw());
                spawnFile.setRaw("world", location.getWorld().getName());

                spawnFile.save();
            }
        });
    }

    /**
     * Load the spawn location
     *
     * @return when the spawn has been loaded
     */
    public LateScheduler<Void> load() {
        LateScheduler<Void> result = new AsyncLateScheduler<>();

        tryAsync(TaskTarget.DATA_LOAD, () -> {
            Element<?> x_string = spawnFile.get("x");
            Element<?> y_string = spawnFile.get("y");
            Element<?> z_string = spawnFile.get("z");
            Element<?> pitch_string = spawnFile.get("pitch");
            Element<?> yaw_string = spawnFile.get("yaw");
            Element<?> world_string = spawnFile.get("world");

            if (!x_string.isPrimitive() && !y_string.isPrimitive() && !z_string.isPrimitive() && !pitch_string.isPrimitive() && !yaw_string.isPrimitive() && !world_string.isPrimitive())
                return;

            ElementPrimitive x_primitive = x_string.getAsPrimitive();
            ElementPrimitive y_primitive = y_string.getAsPrimitive();
            ElementPrimitive z_primitive = z_string.getAsPrimitive();
            ElementPrimitive yaw_primitive = yaw_string.getAsPrimitive();
            ElementPrimitive pitch_primitive = pitch_string.getAsPrimitive();
            ElementPrimitive world_primitive = world_string.getAsPrimitive();

            if (!x_primitive.isNumber() && !y_primitive.isNumber() && z_primitive.isNumber() && !yaw_primitive.isNumber() && pitch_primitive.isNumber() && !world_primitive.isString())
                return;

            try {
                double x = x_primitive.asDouble();
                double y = y_primitive.asDouble();
                double z = z_primitive.asDouble();

                float pitch = pitch_primitive.asFloat();
                float yaw = yaw_primitive.asFloat();

                World world = plugin.getServer().getWorld(world_primitive.asString());

                if (world != null) {
                    spawn_location = new Location(world, x, y, z);
                    spawn_location.setPitch(pitch);
                    spawn_location.setYaw(yaw);

                    result.complete(null);
                }
            } catch (Throwable ignored) {
            }
        });

        return result;
    }
}
