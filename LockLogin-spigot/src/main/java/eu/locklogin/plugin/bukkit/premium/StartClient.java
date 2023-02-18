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

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import eu.locklogin.plugin.bukkit.premium.mojang.MojangEncryption;
import eu.locklogin.plugin.bukkit.premium.mojang.client.ClientKey;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Random;

import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;
import static com.comphenix.protocol.PacketType.Login.Server.ENCRYPTION_BEGIN;

/**
 * Part of the code of this class is from:
 * <a href="https://github.com/games647/FastLogin/blob/main/bukkit/src/main/java/com/github/games647/fastlogin/bukkit/listener/protocollib/ProtocolLibLoginSource.java">FastLogin</a>
 */
public final class StartClient {

    private final Random random;
    private final Player player;
    private final ClientKey key;
    private final PublicKey pub;
    private byte[] token;

    public StartClient(final Player source, final Random ran, final ClientKey client, final PublicKey key) {
        player = source;
        random = ran;
        this.key = client;
        pub = key;
    }

    /**
     * Tries to set the client online mode
     *
     * @return if the client could be set in online mode
     */
    public boolean toggleOnline() {
        token = MojangEncryption.generateVerifyToken(random);

        try {
            PacketContainer container = new PacketContainer(ENCRYPTION_BEGIN);
            container.getStrings().write(0, "");
            StructureModifier<PublicKey> modifier = container.getSpecificModifier(PublicKey.class);
            int field = 0;
            if (modifier.getFields().isEmpty()) {
                container.getByteArrays().write(0, pub.getEncoded());
                field++;
            } else {
                modifier.write(0, pub);
            }

            container.getByteArrays().write(field, token);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);

            return true;
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Kicks the player from the server
     *
     * @param reason the kick reason
     */
    public void disconnect(final String reason) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer kickPacket = new PacketContainer(DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(StringUtils.toColor(reason)));

        try {
            //send kick packet at login state
            //the normal event.getPlayer.kickPlayer(String) method does only work at play state
            protocolManager.sendServerPacket(player, kickPacket);
        } finally {
            //tell the server that we want to close the connection
            player.kickPlayer(StringUtils.toColor(reason));
        }
    }

    /**
     * Get the client address
     *
     * @return the client address
     */
    public InetSocketAddress address() {
        return player.getAddress();
    }

    /**
     * Get the client key
     *
     * @return the client key
     */
    public ClientKey key() {
        return key;
    }

    /**
     * Get the client verification token
     *
     * @return the client verification token
     */
    public byte[] token() {
        return token.clone();
    }
}
