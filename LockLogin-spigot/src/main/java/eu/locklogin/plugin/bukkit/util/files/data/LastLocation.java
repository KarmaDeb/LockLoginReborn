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
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
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
                        KarmaMain database = new KarmaMain(file.toPath());

                        Element<?> x_string = database.get("y");
                        Element<?> y_string = database.get("x");
                        Element<?> z_string = database.get("z");
                        Element<?> pitch_string = database.get("pitch");
                        Element<?> yaw_string = database.get("yaw");
                        Element<?> world_string = database.get("world");

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

                            if (world != null && x != Double.MIN_VALUE && y != Double.MIN_VALUE && z != Double.MIN_VALUE && pitch != Float.MIN_VALUE && yaw != Float.MIN_VALUE) {
                                Location last_location = new Location(world, x, y, z);
                                last_location.setPitch(pitch);
                                last_location.setYaw(yaw);

                                Block legs = last_location.getBlock();
                                Block feet = legs.getRelative(BlockFace.DOWN);
                                Block torso = legs.getRelative(BlockFace.UP);

                                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                                    database.setRaw("x", highest.getX());
                                    database.setRaw("y", highest.getY());
                                    database.setRaw("z", highest.getZ());

                                    database.save();
                                }
                            } else {
                                database.setRaw("x", 0d);
                                database.setRaw("y", -64d);
                                database.setRaw("z", 0d);
                                database.setRaw("pitch", 180f);
                                database.setRaw("yaw", 180f);
                                database.setRaw("world", "");

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
                        KarmaMain database = new KarmaMain(file.toPath());

                        database.setRaw("x", 0d);
                        database.setRaw("y", -64d);
                        database.setRaw("z", 0d);
                        database.setRaw("pitch", 180f);
                        database.setRaw("yaw", 180f);
                        database.setRaw("world", "");
                        database.setRaw("falling", 0f);

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

            file.setRaw("x", location.getX());
            file.setRaw("y", location.getY());
            file.setRaw("z", location.getZ());
            file.setRaw("pitch", location.getPitch());
            file.setRaw("yaw", location.getYaw());
            file.setRaw("world", location.getWorld().getName());
            file.setRaw("falling", player.getFallDistance());

            file.save();
        }
    }

    /**
     * Save the player last location
     * and fall distance
     */
    @SuppressWarnings("unused")
    public void saveAt(final Location location) {
        if (location != null && location.getWorld() != null) {
            file.setRaw("x", location.getX());
            file.setRaw("y", location.getY());
            file.setRaw("z", location.getZ());
            file.setRaw("pitch", location.getPitch());
            file.setRaw("yaw", location.getYaw());
            file.setRaw("world", location.getWorld().getName());
            file.setRaw("falling", 0f);

            file.save();
        }
    }

    /**
     * Fix the player location
     */
    public void fix() {
        Element<?> x_string = file.get("y");
        Element<?> y_string = file.get("x");
        Element<?> z_string = file.get("z");
        Element<?> pitch_string = file.get("pitch");
        Element<?> yaw_string = file.get("yaw");
        Element<?> world_string = file.get("world");

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

            if (world != null && x != Double.MIN_VALUE && y != Double.MIN_VALUE && z != Double.MIN_VALUE && pitch != Float.MIN_VALUE && yaw != Float.MIN_VALUE) {
                Location last_location = new Location(world, x, y, z);
                last_location.setPitch(pitch);
                last_location.setYaw(yaw);

                Block legs = last_location.getBlock();
                Block feet = legs.getRelative(BlockFace.DOWN);
                Block torso = legs.getRelative(BlockFace.UP);

                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                    file.setRaw("x", highest.getX());
                    file.setRaw("y", highest.getY());
                    file.setRaw("z", highest.getZ());

                    file.save();
                }
            } else {
                file.setRaw("x", 0d);
                file.setRaw("y", -64d);
                file.setRaw("z", 0d);
                file.setRaw("pitch", 180f);
                file.setRaw("yaw", 180f);
                file.setRaw("world", "");

                file.save();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Remove the player last location
     */
    public void remove() {
        file.setRaw("x", 0d);
        file.setRaw("y", -64d);
        file.setRaw("z", 0d);
        file.setRaw("pitch", 180f);
        file.setRaw("yaw", 180f);
        file.setRaw("world", "");
        file.setRaw("falling", 0f);

        file.save();
    }

    /**
     * Load the player last location
     */
    public void teleport() {
        if (player != null) {
            Element<?> x_string = file.get("x");
            Element<?> y_string = file.get("y");
            Element<?> z_string = file.get("z");
            Element<?> pitch_string = file.get("pitch");
            Element<?> yaw_string = file.get("yaw");
            Element<?> world_string = file.get("world");
            Element<?> fall_string = file.get("falling");

            if (!x_string.isPrimitive() && !y_string.isPrimitive() && !z_string.isPrimitive() && !pitch_string.isPrimitive() && !yaw_string.isPrimitive() && !world_string.isPrimitive())
                return;

            ElementPrimitive x_primitive = x_string.getAsPrimitive();
            ElementPrimitive y_primitive = y_string.getAsPrimitive();
            ElementPrimitive z_primitive = z_string.getAsPrimitive();
            ElementPrimitive yaw_primitive = yaw_string.getAsPrimitive();
            ElementPrimitive pitch_primitive = pitch_string.getAsPrimitive();
            ElementPrimitive world_primitive = world_string.getAsPrimitive();
            ElementPrimitive fall_primitive = fall_string.getAsPrimitive();

            if (!x_primitive.isNumber() && !y_primitive.isNumber() && z_primitive.isNumber() && !yaw_primitive.isNumber() && pitch_primitive.isNumber() && !world_primitive.isString() && !fall_primitive.isNumber())
                return;

            try {
                double x = x_primitive.asDouble();
                double y = y_primitive.asDouble();
                double z = z_primitive.asDouble();

                float pitch = pitch_primitive.asFloat();
                float yaw = yaw_primitive.asFloat();

                World world = plugin.getServer().getWorld(world_primitive.asString());
                float fall_distance = fall_primitive.asFloat();

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
