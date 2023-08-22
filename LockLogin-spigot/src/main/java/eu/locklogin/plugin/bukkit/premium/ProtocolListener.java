/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package eu.locklogin.plugin.bukkit.premium;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.*;
import com.github.games647.craftapi.model.skin.SkinProperty;
import com.github.games647.craftapi.model.skin.Textures;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.premium.handler.EncryptionHandler;
import eu.locklogin.plugin.bukkit.premium.handler.LoginHandler;
import eu.locklogin.plugin.bukkit.premium.mojang.MojangEncryption;
import eu.locklogin.plugin.bukkit.premium.mojang.client.ClientKey;
import ml.karmaconfigs.api.bukkit.server.BukkitServer;
import ml.karmaconfigs.api.bukkit.server.Version;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.comphenix.protocol.PacketType.Login.Client.ENCRYPTION_BEGIN;
import static com.comphenix.protocol.PacketType.Login.Client.START;

/**
 * Part of the code of this class is from:
 * <a href="https://github.com/games647/FastLogin/blob/main/bukkit/src/main/java/com/github/games647/fastlogin/bukkit/listener/protocollib/ProtocolLibListener.java">FastLogin</a>
 */
public class ProtocolListener extends PacketAdapter {

    private final static PluginMessages messages = CurrentPlatform.getMessages();
    private final SecureRandom random = new SecureRandom();
    private final KeyPair keyPair = MojangEncryption.generatePair();

    public final static Map<InetSocketAddress, LoginSession> sessions = new ConcurrentHashMap<>();


    public ProtocolListener() {
        super(params()
                .plugin(LockLogin.plugin)
                .types(START, ENCRYPTION_BEGIN)
                .optionAsync());
    }

    public static void register() {
        LockLogin.plugin.console().send("Registering ProtocolLib listener", Level.OK);
        ProtocolLibrary.getProtocolManager().getAsynchronousManager()
                .registerAsyncHandler(new ProtocolListener()).start();
    }

    @Override
    public void onPacketReceiving(final PacketEvent event) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (event.isCancelled() || !Manager.isInitialized() || keyPair == null || !config.enablePremium() || config.isBungeeCord()) {
            /*
            We will ignore if the plugin is not fully started or the
            packet got cancelled in order to avoid errors
             */
            return;
        }

        Player player = event.getPlayer();
        InetSocketAddress address = player.getAddress();

        PacketType type = event.getPacketType();
        PacketContainer container = event.getPacket();

        if (type == START) {
            String name = getUsername(container);

            Optional<ClientKey> client = Optional.empty();
            if (BukkitServer.isUnder(Version.v1_19_3)) {
                try {
                    Optional<Optional<WrappedProfilePublicKey.WrappedProfileKeyData>> profile = container.getOptionals(BukkitConverters.getWrappedPublicKeyDataConverter()).optionRead(0);
                    client = profile.flatMap(Function.identity()).flatMap((data) -> {
                        Instant expiration = data.getExpireTime();
                        PublicKey key = data.getKey();
                        byte[] sign = data.getSignature();
                        return Optional.of(ClientKey.newInstance(expiration, key, sign));
                    });

                    Optional<UUID> clientId = container.getOptionals(Converters.passthrough(UUID.class)).readSafely(1);
                    if (clientId.isPresent() && client.isPresent()) {
                        ClientKey key = client.get();
                        if (!MojangEncryption.isValidClient(key, key.expiration(), clientId.get())) {
                            player.kickPlayer(StringUtils.toColor(messages.premiumFailSession()));
                            return;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            event.getAsyncMarker().incrementProcessingDelay();
            Runnable task = new LoginHandler(random, player, event, name, client.orElse(null), keyPair.getPublic());
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            byte[] sharedSecret = event.getPacket().getByteArrays().read(0);
            if (sessions.containsKey(address)) {
                LoginSession session = sessions.get(address);

                byte[] token = session.getToken();
                ClientKey key = session.getKey();
                if (verifyNonce(player, container, key, token)) {
                    event.getAsyncMarker().incrementProcessingDelay();

                    Runnable runnable = new EncryptionHandler(event, player, session, sharedSecret, keyPair);
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
                } else {
                    player.kickPlayer(StringUtils.toColor(messages.premiumFailEncryption()));
                }
            } else {
                player.kickPlayer(StringUtils.toColor(messages.premiumFailPrecocious()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean verifyNonce(Player sender, PacketContainer packet, ClientKey clientPublicKey, byte[] expectedToken) {
        MinecraftVersion c = new MinecraftVersion("1.19.0");
        MinecraftVersion c2 = new MinecraftVersion("1.19.3");
        if (c.atOrAbove()
                && !c2.atOrAbove()) {
            Either<byte[], ?> either = packet.getSpecificModifier(Either.class).read(0);
            if (clientPublicKey == null) {
                Optional<byte[]> left = either.left();
                if (!left.isPresent()) {
                    return false;
                }

                return MojangEncryption.verifyIntegrity(expectedToken, keyPair.getPrivate(), left.get());
            } else {
                Optional<?> optSignatureData = either.right();
                if (!optSignatureData.isPresent()) {
                    return false;
                }

                Object signatureData = optSignatureData.get();
                long salt = FuzzyReflection.getFieldValue(signatureData, Long.TYPE, true);
                byte[] signature = FuzzyReflection.getFieldValue(signatureData, byte[].class, true);

                PublicKey publicKey = clientPublicKey.key();
                return MojangEncryption.verifyClientIntegrity(expectedToken, publicKey, salt, signature);
            }
        } else {
            byte[] nonce = packet.getByteArrays().read(1);
            return MojangEncryption.verifyIntegrity(expectedToken, keyPair.getPrivate(), nonce);
        }
    }

    private String getUsername(PacketContainer packet) {
        WrappedGameProfile profile = packet.getGameProfiles().readSafely(0);
        if (profile == null) {
            return packet.getStrings().read(0);
        }

        //player.getName() won't work at this state
        return profile.getName();
    }

    public static void trySkin(final Player player) {
        for (InetSocketAddress address : sessions.keySet()) {
            LoginSession session = sessions.get(address);
            if (session.getId() != null && session.getId().equals(player.getUniqueId())) {
                sessions.remove(address);

                SkinProperty skin = session.getSkin();

                if (skin != null) {
                    WrappedGameProfile wgp = WrappedGameProfile.fromPlayer(player);

                    WrappedSignedProperty skinProperty = WrappedSignedProperty.fromValues(Textures.KEY, skin.getValue(), skin.getSignature());
                    wgp.getProperties().put(Textures.KEY, skinProperty);
                }
                break;
            }
        }
    }
}
