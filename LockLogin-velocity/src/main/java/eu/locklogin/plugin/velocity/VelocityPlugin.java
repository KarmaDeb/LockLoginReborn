package eu.locklogin.plugin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.velocity.KarmaPlugin;
import org.bstats.velocity.Metrics;

@Plugin(
        id = "locklogin",
        name = "LockLogin",
        version = "1.13.20",
        authors = {"KarmaDev"},
        description =
                "LockLogin is an advanced login plugin, one of the most secure available, with tons of features. " +
                "It has a lot of customization options to not say almost everything is customizable. Regular updates " +
                "and one of the bests discord supports ( according to spigotmc reviews ). LockLogin is a plugin " +
                "always open to new feature requests, and bug reports. More than a plugin, a plugin you can contribute indirectly; " +
                "A community plugin for the plugin community.",
        url = "https://locklogin.eu/",
        dependencies = {
                @com.velocitypowered.api.plugin.Dependency(id = "anotherbarelycodedkarmaplugin")
        })
public class VelocityPlugin {

    private final ProxyServer server;

    static Metrics.Factory factory;
    static KarmaPlugin plugin;

    @Inject
    public VelocityPlugin(final ProxyServer sv, final Metrics.Factory fact) {
        KarmaAPI.install();

        server = sv;
        factory = fact;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyInitializeEvent e) {
        server.getPluginManager().getPlugin("locklogin").ifPresent((container) -> {
            plugin = new Main(server, container);
            plugin.enable();

            CurrentPlatform.setOnline(server.getConfiguration().isOnlineMode());
        });
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyShutdownEvent e) {
        plugin.disable();
    }
}