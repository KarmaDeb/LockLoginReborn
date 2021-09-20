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
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

public final class Spawn {

    private final static KarmaFile spawnFile = new KarmaFile(plugin, "spawn.lldb", "data", "location");
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
     * Teleport the player to the spawn
     * location
     */
    public void teleport(final Player player) {
        trySync(() -> {
            assert spawn_location.getWorld() != null;

            Block middle_down = spawn_location.getBlock().getRelative(BlockFace.UP);
            Block middle_up = middle_down.getRelative(BlockFace.UP);

            if (!middle_down.getType().equals(Material.AIR) && !middle_up.getType().equals(Material.AIR)) {
                Block highest = spawn_location.getWorld().getHighestBlockAt(spawn_location);
                spawn_location = highest.getLocation().add(0D, 1D, 0D);
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> player.teleport(spawn_location));
        });
    }

    /**
     * Save the spawn location
     */
    public void save(final @NotNull Location location) {
        tryAsync(() -> {
            if (location.getWorld() != null) {
                spawnFile.set("X", location.getX());
                spawnFile.set("Y", location.getY());
                spawnFile.set("Z", location.getZ());
                spawnFile.set("PITCH", location.getPitch());
                spawnFile.set("YAW", location.getYaw());
                spawnFile.set("WORLD", location.getWorld().getName());
            }
        });
    }

    /**
     * Load the spawn location
     */
    public void load() {
        String x_string = spawnFile.getString("X", "");
        String y_string = spawnFile.getString("Y", "");
        String z_string = spawnFile.getString("Z", "");
        String pitch_string = spawnFile.getString("PITCH", "");
        String yaw_string = spawnFile.getString("YAW", "");
        String world_string = spawnFile.getString("WORLD", "");

        if (isNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
            return;

        try {
            double x = Double.parseDouble(x_string);
            double y = Double.parseDouble(y_string);
            double z = Double.parseDouble(z_string);

            float pitch = Float.parseFloat(pitch_string);
            float yaw = Float.parseFloat(yaw_string);

            World world = plugin.getServer().getWorld(world_string);

            if (world == null) {
                try {
                    console.send("Creating world {0} because is set as spawn location", Level.INFO, world_string);
                    world = plugin.getServer().createWorld(WorldCreator.name(world_string));
                } catch (Throwable ex) {
                    console.send("Failed to create world {0} ( {1} )", Level.GRAVE, world_string, ex.fillInStackTrace());
                    return;
                }
            }

            if (world != null) {
                spawn_location = new Location(world, x, y, z);
                spawn_location.setPitch(pitch);
                spawn_location.setYaw(yaw);
            }
        } catch (Throwable ignored) {
        }
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
            String x_string = spawnFile.getString("X", "");
            String y_string = spawnFile.getString("Y", "");
            String z_string = spawnFile.getString("Z", "");
            String pitch_string = spawnFile.getString("PITCH", "");
            String yaw_string = spawnFile.getString("YAW", "");
            String world_string = spawnFile.getString("WORLD", "");

            if (!isNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string)) {
                try {
                    double x = Double.parseDouble(x_string);
                    double y = Double.parseDouble(y_string);
                    double z = Double.parseDouble(z_string);

                    float pitch = Float.parseFloat(pitch_string);
                    float yaw = Float.parseFloat(yaw_string);

                    World world = plugin.getServer().getWorld(world_string);

                    if (world == null) {
                        try {
                            console.send("Creating world {0} because is set as spawn location", Level.INFO, world_string);
                            world = plugin.getServer().createWorld(WorldCreator.name(world_string));
                        } catch (Throwable ex) {
                            console.send("Failed to create world {0} ( {1} )", Level.GRAVE, world_string, ex.fillInStackTrace());
                        }
                    }

                    if (world != null) {
                        spawn_location = new Location(world, x, y, z);
                        spawn_location.setPitch(pitch);
                        spawn_location.setYaw(yaw);

                        return player.getLocation().distance(spawn_location) >= config.spawnDistance();
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        return true;
    }
}
