package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data;

import ml.karmaconfigs.api.bukkit.KarmaFile;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

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
     * Save the player last location
     * and fall distance
     */
    public final void save() {
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
    }

    /**
     * Load the player last location
     */
    public final void teleport() {
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
