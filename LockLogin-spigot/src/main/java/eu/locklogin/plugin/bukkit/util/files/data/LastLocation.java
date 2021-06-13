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

import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import eu.locklogin.api.account.AccountID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.File;

import static eu.locklogin.plugin.bukkit.LockLogin.isNullOrEmpty;
import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public final class LastLocation {

    private final KarmaFile file;

    private final Player player;

    /**
     * Initialize the last location
     *
     * @param _player the player
     */
    public LastLocation(final Player _player) {
        player = _player;

        file = new KarmaFile(plugin, player.getUniqueId().toString().replace("-", "") + ".lldb", "data", "location");
    }

    /**
     * Initialize the last location
     *
     * @param account the player
     */
    public LastLocation(final AccountID account) {
        player = null;

        file = new KarmaFile(plugin, account.getId().replace("-", "") + ".lldb", "data", "location");
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
                    //I didn't thought I would need this when I made it
                    //I was wrong...
                    if (FileUtilities.isKarmaFile(file)) {
                        KarmaFile database = new KarmaFile(file);

                        String x_string = database.getString("X", "");
                        String y_string = database.getString("Y", "");
                        String z_string = database.getString("Z", "");
                        String pitch_string = database.getString("PITCH", "");
                        String yaw_string = database.getString("YAW", "");
                        String world_string = database.getString("WORLD", "");

                        if (isNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
                            return;

                        try {
                            double x = Double.parseDouble(x_string);
                            double y = Double.parseDouble(y_string);
                            double z = Double.parseDouble(z_string);

                            float pitch = Float.parseFloat(pitch_string);
                            float yaw = Float.parseFloat(yaw_string);

                            World world = plugin.getServer().getWorld(world_string);

                            if (world != null) {
                                Location last_location = new Location(world, x, y, z);
                                last_location.setPitch(pitch);
                                last_location.setYaw(yaw);

                                Block legs = last_location.getBlock();
                                Block feet = legs.getRelative(BlockFace.DOWN);
                                Block torso = legs.getRelative(BlockFace.UP);

                                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                                    database.set("X", highest.getX());
                                    database.set("Y", highest.getY());
                                    database.set("Z", highest.getZ());
                                }
                            } else {
                                database.set("X", "");
                                database.set("Y", "");
                                database.set("Z", "");
                                database.set("PITCH", "");
                                database.set("YAW", "");
                                database.set("WORLD", "");
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
                        KarmaFile database = new KarmaFile(file);

                        database.set("X", "");
                        database.set("Y", "");
                        database.set("Z", "");
                        database.set("PITCH", "");
                        database.set("YAW", "");
                        database.set("WORLD", "");
                        database.set("FALLING", "");
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
    public final void save() {
        if (player != null) {
            Location location = player.getLocation();
            assert location.getWorld() != null;

            file.set("X", location.getX());
            file.set("Y", location.getY());
            file.set("Z", location.getZ());
            file.set("PITCH", location.getPitch());
            file.set("YAW", location.getYaw());
            file.set("WORLD", location.getWorld().getName());
            file.set("FALLING", player.getFallDistance());
        }
    }

    /**
     * Fix the player location
     */
    public final void fix() {
        String x_string = file.getString("X", "");
        String y_string = file.getString("Y", "");
        String z_string = file.getString("Z", "");
        String pitch_string = file.getString("PITCH", "");
        String yaw_string = file.getString("YAW", "");
        String world_string = file.getString("WORLD", "");

        if (isNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
            return;

        try {
            double x = Double.parseDouble(x_string);
            double y = Double.parseDouble(y_string);
            double z = Double.parseDouble(z_string);

            float pitch = Float.parseFloat(pitch_string);
            float yaw = Float.parseFloat(yaw_string);

            World world = plugin.getServer().getWorld(world_string);

            if (world != null) {
                Location last_location = new Location(world, x, y, z);
                last_location.setPitch(pitch);
                last_location.setYaw(yaw);

                Block legs = last_location.getBlock();
                Block feet = legs.getRelative(BlockFace.DOWN);
                Block torso = legs.getRelative(BlockFace.UP);

                if (blockNotSafe(legs) || blockNotSafe(feet) || blockNotSafe(torso) || isSuffocating(legs, torso)) {
                    Location highest = world.getHighestBlockAt(last_location).getLocation().add(0D, 1D, 0D);

                    file.set("X", highest.getX());
                    file.set("Y", highest.getY());
                    file.set("Z", highest.getZ());
                }
            } else {
                file.set("X", "");
                file.set("Y", "");
                file.set("Z", "");
                file.set("PITCH", "");
                file.set("YAW", "");
                file.set("WORLD", "");
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Remove the player last location
     */
    public final void remove() {
        file.set("X", "");
        file.set("Y", "");
        file.set("Z", "");
        file.set("PITCH", "");
        file.set("YAW", "");
        file.set("WORLD", "");
        file.set("FALLING", "");
    }

    /**
     * Load the player last location
     */
    public final void teleport() {
        if (player != null) {
            String x_string = file.getString("X", "");
            String y_string = file.getString("Y", "");
            String z_string = file.getString("Z", "");
            String pitch_string = file.getString("PITCH", "");
            String yaw_string = file.getString("YAW", "");
            String world_string = file.getString("WORLD", "");

            if (isNullOrEmpty(x_string, y_string, z_string, pitch_string, yaw_string, world_string))
                return;

            try {
                double x = Double.parseDouble(x_string);
                double y = Double.parseDouble(y_string);
                double z = Double.parseDouble(z_string);

                float pitch = Float.parseFloat(pitch_string);
                float yaw = Float.parseFloat(yaw_string);

                float fall_distance = (float) file.getDouble("FALLING", 0F);

                World world = plugin.getServer().getWorld(world_string);

                if (world != null) {
                    Location last_location = new Location(world, x, y, z);
                    last_location.setPitch(pitch);
                    last_location.setYaw(yaw);

                    //Store the player fall distance so when he joins back
                    //it gets restored and fall damage won't be prevented
                    player.setFallDistance(fall_distance);
                    player.teleport(last_location);

                    //Unset the last location
                    remove();
                }
            } catch (Throwable ignored) {}
        }
    }
}
