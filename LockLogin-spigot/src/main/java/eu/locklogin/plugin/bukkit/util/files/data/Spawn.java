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
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
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
            KarmaElement x_string = spawnFile.get("X", null);
            KarmaElement y_string = spawnFile.get("Y", null);
            KarmaElement z_string = spawnFile.get("Z", null);
            KarmaElement pitch_string = spawnFile.get("PITCH", null);
            KarmaElement yaw_string = spawnFile.get("YAW", null);
            KarmaElement world_string = spawnFile.get("WORLD", null);

            if (!StringUtils.areNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string)) {
                try {
                    double x = x_string.getObjet().getNumber().doubleValue();
                    double y = y_string.getObjet().getNumber().doubleValue();
                    double z = z_string.getObjet().getNumber().doubleValue();

                    float pitch = pitch_string.getObjet().getNumber().floatValue();
                    float yaw = yaw_string.getObjet().getNumber().floatValue();

                    World world = plugin.getServer().getWorld(world_string.getObjet().getString());

                    if (world == null) {
                        try {
                            console.send("Creating world {0} because is set as spawn location", Level.INFO, world_string.getObjet().getString());
                            world = plugin.getServer().createWorld(WorldCreator.name(world_string.getObjet().getString()));
                        } catch (Throwable ex) {
                            console.send("Failed to create world {0} ( {1} )", Level.GRAVE, world_string.getObjet().getString(), ex.fillInStackTrace());
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
                spawnFile.set("X", new KarmaObject(location.getX()));
                spawnFile.set("Y", new KarmaObject(location.getY()));
                spawnFile.set("Z", new KarmaObject(location.getZ()));
                spawnFile.set("PITCH", new KarmaObject(location.getPitch()));
                spawnFile.set("YAW", new KarmaObject(location.getYaw()));
                spawnFile.set("WORLD", new KarmaObject(location.getWorld().getName()));

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
            KarmaElement x_string = spawnFile.get("X", null);
            KarmaElement y_string = spawnFile.get("Y", null);
            KarmaElement z_string = spawnFile.get("Z", null);
            KarmaElement pitch_string = spawnFile.get("PITCH", null);
            KarmaElement yaw_string = spawnFile.get("YAW", null);
            KarmaElement world_string = spawnFile.get("WORLD", null);

            if (StringUtils.areNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
                return;

            try {
                double x = x_string.getObjet().getNumber().doubleValue();
                double y = y_string.getObjet().getNumber().doubleValue();
                double z = z_string.getObjet().getNumber().doubleValue();

                float pitch = pitch_string.getObjet().getNumber().floatValue();
                float yaw = yaw_string.getObjet().getNumber().floatValue();

                World world = plugin.getServer().getWorld(world_string.getObjet().getString());

                if (world == null) {
                    try {
                        console.send("Creating world {0} because is set as spawn location", Level.INFO, world_string.getObjet().getString());
                        world = plugin.getServer().createWorld(WorldCreator.name(world_string.getObjet().getString()));
                    } catch (Throwable ex) {
                        console.send("Failed to create world {0} ( {1} )", Level.GRAVE, world_string, ex.fillInStackTrace());
                        return;
                    }
                }

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
