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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.plugin.bukkit.TaskTarget;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.File;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static eu.locklogin.plugin.bukkit.LockLogin.trySync;

public final class LastLocation {

    private final KarmaMain file;

    private final Player player;

    /**
     * Initialize the last location
     *
     * @param _player the player
     */
    public LastLocation(final Player _player) {
        player = _player;

        file = new KarmaMain(plugin, player.getUniqueId().toString().replace("-", "") + ".lldb", "data", "location");
    }

    /**
     * Initialize the last location
     *
     * @param account the player
     */
    public LastLocation(final AccountID account) {
        player = null;

        file = new KarmaMain(plugin, account.getId().replace("-", "") + ".lldb", "data", "location");
    }

    /**
     * Fix the player last location
     */
    public static void fixAll() {
        //As this process will modify all the files, it can cause a lot of lag
        //on big servers such networks. Async is the answer to all our lag problems
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File locations_database = new File(plugin.getDataFolder() + File.separator + "data", "locations");
            File[] files = locations_database.listFiles();

            if (files != null) {
                for (File file : files) {
                    //I didn't think I would need this when I made it
                    //I was wrong...
                    if (FileUtilities.isKarmaFile(file)) {
                        KarmaMain database = new KarmaMain(plugin, file.toPath());

                        KarmaElement x_string = database.get("y", null);
                        KarmaElement y_string = database.get("x", null);
                        KarmaElement z_string = database.get("z", null);
                        KarmaElement pitch_string = database.get("pitch", null);
                        KarmaElement yaw_string = database.get("yaw", null);
                        KarmaElement world_string = database.get("world", null);

                        if (StringUtils.areNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
                            return;

                        try {
                            double x = x_string.getObjet().getNumber().doubleValue();
                            double y = y_string.getObjet().getNumber().doubleValue();
                            double z = z_string.getObjet().getNumber().doubleValue();

                            float pitch = pitch_string.getObjet().getNumber().floatValue();
                            float yaw = yaw_string.getObjet().getNumber().floatValue();

                            World world = plugin.getServer().getWorld(world_string.getObjet().getString());

                            if (world != null && x != Double.MIN_VALUE && y != Double.MIN_VALUE && z != Double.MIN_VALUE && pitch != Float.MIN_VALUE && yaw != Float.MIN_VALUE) {
                                Location last_location = new Location(world, x, y, z);
                                last_location.setPitch(pitch);
                                last_location.setYaw(yaw);

                                Block legs = last_location.getBlock();
                                Block feet = legs.getRelative(BlockFace.DOWN);
                                Block torso = legs.getRelative(BlockFace.UP);

                                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                                    database.set("x", new KarmaObject(highest.getX()));
                                    database.set("y", new KarmaObject(highest.getY()));
                                    database.set("z", new KarmaObject(highest.getZ()));

                                    database.save();
                                }
                            } else {
                                database.set("x", new KarmaObject(Double.MIN_VALUE));
                                database.set("y", new KarmaObject(Double.MIN_VALUE));
                                database.set("z", new KarmaObject(Double.MIN_VALUE));
                                database.set("pitch", new KarmaObject(Float.MIN_VALUE));
                                database.set("yaw", new KarmaObject(Float.MIN_VALUE));
                                database.set("world", new KarmaObject(""));

                                database.save();
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        });
    }

    /**
     * Fix the player last location
     */
    public static void removeAll() {
        //As this process will modify all the files, it can cause a lot of lag
        //on big servers such networks. Async is the answer to all our lag problems
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File locations_database = new File(plugin.getDataFolder() + File.separator + "data", "locations");
            File[] files = locations_database.listFiles();

            if (files != null) {
                for (File file : files) {
                    //I didn't thought I would need this when I made it
                    //I was wrong...
                    if (FileUtilities.isKarmaFile(file)) {
                        KarmaMain database = new KarmaMain(plugin, file.toPath());

                        database.set("x", new KarmaObject(Double.MIN_VALUE));
                        database.set("y", new KarmaObject(Double.MIN_VALUE));
                        database.set("z", new KarmaObject(Double.MIN_VALUE));
                        database.set("pitch", new KarmaObject(Float.MIN_VALUE));
                        database.set("yaw", new KarmaObject(Float.MIN_VALUE));
                        database.set("world", new KarmaObject(""));
                        database.set("falling", new KarmaObject(Float.MIN_VALUE));

                        database.save();
                    }
                }
            }
        });
    }

    /**
     * Get if the block is safe or not
     *
     * @param block the block
     * @return if the block is safe
     */
    private static boolean blockNotSafe(final Block block) {
        Material type = block.getType();

        return type.equals(Material.LAVA) || type.name().contains("MAGMA") || type.name().contains("FIRE");
    }

    /**
     * Get if the blocks will suffocate the player
     *
     * @param blocks the blocks
     * @return if the blocks will suffocate the player
     */
    private static boolean isSuffocating(final Block... blocks) {
        for (Block block : blocks) {
            if (!block.getType().equals(Material.AIR))
                return true;
        }

        return false;
    }

    /**
     * Save the player last location
     * and fall distance
     */
    public void save() {
        if (player != null) {
            Location location = player.getLocation();
            assert location.getWorld() != null;

            file.set("X", new KarmaObject(location.getX()));
            file.set("Y", new KarmaObject(location.getY()));
            file.set("Z", new KarmaObject(location.getZ()));
            file.set("PITCH", new KarmaObject(location.getPitch()));
            file.set("YAW", new KarmaObject(location.getYaw()));
            file.set("WORLD", new KarmaObject(location.getWorld().getName()));
            file.set("FALLING", new KarmaObject(player.getFallDistance()));

            file.save();
        }
    }

    /**
     * Save the player last location
     * and fall distance
     */
    public void saveAt(final Location location) {
        if (location != null && location.getWorld() != null) {
            file.set("X", new KarmaObject(location.getX()));
            file.set("Y", new KarmaObject(location.getY()));
            file.set("Z", new KarmaObject(location.getZ()));
            file.set("PITCH", new KarmaObject(location.getPitch()));
            file.set("YAW", new KarmaObject(location.getYaw()));
            file.set("WORLD", new KarmaObject(location.getWorld().getName()));
            file.set("FALLING", new KarmaObject(0f));

            file.save();
        }
    }

    /**
     * Fix the player location
     */
    public void fix() {
        KarmaElement x_string = file.get("X", null);
        KarmaElement y_string = file.get("Y", null);
        KarmaElement z_string = file.get("Z", null);
        KarmaElement pitch_string = file.get("PITCH", null);
        KarmaElement yaw_string = file.get("YAW", null);
        KarmaElement world_string = file.get("WORLD", null);

        if (StringUtils.areNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
            return;

        try {
            double x = x_string.getObjet().getNumber().doubleValue();
            double y = y_string.getObjet().getNumber().doubleValue();
            double z = z_string.getObjet().getNumber().doubleValue();

            float pitch = pitch_string.getObjet().getNumber().floatValue();
            float yaw = yaw_string.getObjet().getNumber().floatValue();

            World world = plugin.getServer().getWorld(world_string.getObjet().getString());

            if (world != null && x != Double.MIN_VALUE && y != Double.MIN_VALUE && z != Double.MIN_VALUE && pitch != Float.MIN_VALUE && yaw != Float.MIN_VALUE) {
                Location last_location = new Location(world, x, y, z);
                last_location.setPitch(pitch);
                last_location.setYaw(yaw);

                Block legs = last_location.getBlock();
                Block feet = legs.getRelative(BlockFace.DOWN);
                Block torso = legs.getRelative(BlockFace.UP);

                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                    file.set("X", new KarmaObject(highest.getX()));
                    file.set("Y", new KarmaObject(highest.getY()));
                    file.set("Z", new KarmaObject(highest.getZ()));

                    file.save();
                }
            } else {
                file.set("X", new KarmaObject(Double.MIN_VALUE));
                file.set("Y", new KarmaObject(Double.MIN_VALUE));
                file.set("Z", new KarmaObject(Double.MIN_VALUE));
                file.set("PITCH", new KarmaObject(Float.MIN_VALUE));
                file.set("YAW", new KarmaObject(Float.MIN_VALUE));
                file.set("WORLD", new KarmaObject(""));

                file.save();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Remove the player last location
     */
    public void remove() {
        file.set("X", new KarmaObject(Double.MIN_VALUE));
        file.set("Y", new KarmaObject(Double.MIN_VALUE));
        file.set("Z", new KarmaObject(Double.MIN_VALUE));
        file.set("PITCH", new KarmaObject(Float.MIN_VALUE));
        file.set("YAW", new KarmaObject(Float.MIN_VALUE));
        file.set("WORLD", new KarmaObject(""));
        file.set("FALLING", new KarmaObject(Float.MIN_VALUE));

        file.save();
    }

    /**
     * Load the player last location
     */
    public void teleport() {
        if (player != null) {
            KarmaElement x_string = file.get("X", null);
            KarmaElement y_string = file.get("Y", null);
            KarmaElement z_string = file.get("Z", null);
            KarmaElement pitch_string = file.get("PITCH", null);
            KarmaElement yaw_string = file.get("YAW", null);
            KarmaElement world_string = file.get("WORLD", null);
            KarmaElement fall_string = file.get("FALLING", null);

            if (StringUtils.areNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string, fall_string))
                return;

            try {
                double x = x_string.getObjet().getNumber().doubleValue();
                double y = y_string.getObjet().getNumber().doubleValue();
                double z = z_string.getObjet().getNumber().doubleValue();

                float pitch = pitch_string.getObjet().getNumber().floatValue();
                float yaw = yaw_string.getObjet().getNumber().floatValue();

                float f_dist = fall_string.getObjet().getNumber().floatValue();
                if (f_dist == Float.MIN_VALUE)
                    f_dist = 0;

                World world = plugin.getServer().getWorld(world_string.getObjet().getString());

                float fall_distance = f_dist;
                if (world != null && x != Double.MIN_VALUE && y != Double.MIN_VALUE && z != Double.MIN_VALUE && yaw != Float.MIN_VALUE && pitch != Float.MIN_VALUE) {
                    Location last_location = new Location(world, x, y, z);
                    last_location.setPitch(pitch);
                    last_location.setYaw(yaw);

                    //Store the player fall distance so when he joins back
                    //it gets restored and fall damage won't be prevented
                    trySync(TaskTarget.TELEPORT, () -> {
                        player.setFallDistance(fall_distance);
                        player.teleport(last_location);
                    });

                    //Unset the last location
                    remove();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
