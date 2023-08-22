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

package eu.locklogin.plugin.bukkit.premium.mojang;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.google.common.primitives.Longs;
import eu.locklogin.plugin.bukkit.Main;
import eu.locklogin.plugin.bukkit.premium.mojang.client.ClientKey;
import ml.karmaconfigs.api.common.utils.enums.Level;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static java.util.Base64.Encoder;

/**
 * Part of the code of this class is from:
 * <a href="https://github.com/games647/FastLogin/blob/main/bukkit/src/main/java/com/github/games647/fastlogin/bukkit/listener/protocollib/EncryptionUtil.java">FastLogin</a>
 */
public final class MojangEncryption {

    private static boolean warned = false;
    private static boolean shared_warned = false;
    private static boolean valid_warned = false;
    private static boolean nonce_warned = false;
    private static boolean signed_nonce_warned = false;

    private final static PublicKey MOJANG_KEY;
    private final static Encoder ENCODER = Base64.getMimeEncoder(76, new byte[]{10});

    /*
     * Basically we will load the session key in order to be
     * able to validate a client has connected to our server. If we
     * fail this task, the server won't be able to validate this and
     * so, it won't be able to mark a client as premium
     */
    static {
        PublicKey temporal_key = null;
        try {
            URL key_dir = Main.class.getClassLoader().getResource("yggdrasil_session_pubkey.der");
            byte[] key = Resources.toByteArray(key_dir);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(key);

            temporal_key = KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            plugin.console().send("LockLogin tried to load the mojang session key but failed. Don't expect premium support", Level.GRAVE);
            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to load mojang session key");
        }

        MOJANG_KEY = temporal_key;
    }

    /**
     * Generate a keypair
     *
     * @return the keypair
     * We will generate a key pair in order to allow client
     * authentication. When a client connects, and we validate
     * he's premium, we will generate then their keypair
     * to authenticate them into mojang servers
     */
    public static KeyPair generatePair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1_024);

            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            if (!warned) {
                warned = true;
                plugin.console().send("An unexpected exception has raised to the java vm. It seems that your computer is not compatible with the RSA hashing algorithm. We won't be able to perform advanced security operations on your server", Level.GRAVE);
            }

            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to generate keypair");
            return null;
        }
    }

    /**
     * Generate a random auth token
     *
     * @param random the generator
     * @return the random token
     * <br>
     * We will use this in order to validate we are "speaking" with the exact
     * same client
     */
    public static byte[] generateVerifyToken(final Random random) {
        byte[] token = new byte[4];
        random.nextBytes(token);

        return token;
    }

    /**
     * Generate the server id
     *
     * @param id the current session id
     * @param secret the shared secret key between client and server
     * @param pub the public key of the server
     * @return the server id
     * <br>
     * We use this generated ID in order to validate a client is actually connected
     * to us.
     */
    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public static String getServerId(final String id, final SecretKey secret, final PublicKey pub) {
        Hasher hasher = Hashing.sha1().newHasher();

        hasher.putBytes(id.getBytes(StandardCharsets.UTF_8));
        hasher.putBytes(secret.getEncoded());
        hasher.putBytes(pub.getEncoded());

        byte[] hash = hasher.hash().asBytes();
        return new BigInteger(hash).toString(16);
    }

    /**
     * Get the shared key and decrypts it in order to
     * we use it
     *
     * @param key the key
     * @param shared the shared key
     * @return the de-coded shared key
     */
    public static SecretKey getShared(final PrivateKey key, final byte[] shared) {
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(shared);
            return new SecretKeySpec(decrypted, "AES");
        } catch (
                NoSuchPaddingException |
                IllegalBlockSizeException |
                NoSuchAlgorithmException |
                BadPaddingException |
                InvalidKeyException ex) {
            if (!shared_warned) {
                shared_warned = true;
                plugin.console().send("It seems that your server is not compatible with client encoding ({0}) or AES. Don't expect premium support", Level.WARNING, key.getAlgorithm());
            }

            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to decrypt shared key");
        }

        return null;
    }

    /**
     * Verify if the client is who he's saying to be
     *
     * @param client the client key
     * @param stamp the stamp to check with the key, so we
     *              can validate if the key is expired or not
     * @param id the client premium UUID
     * @return if the client is a valid client
     */
    public static boolean isValidClient(final ClientKey client, final Instant stamp, final UUID id) {
        if (client.isExpired(stamp) || MOJANG_KEY == null)
            return false;

        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(MOJANG_KEY);

            byte[] singable;
            if (id == null) {
                long expiration = client.expiration().toEpochMilli();
                String encoded = ENCODER.encodeToString(client.key().getEncoded());
                singable = (expiration + "-----BEGIN RSA PUBLIC KEY-----\n" + encoded + "\n-----END RSA PUBLIC KEY-----\n")
                        .getBytes(StandardCharsets.US_ASCII);
            } else {
                byte[] key = client.key().getEncoded();
                singable = ByteBuffer.allocate(key.length + 24)
                        .putLong(id.getMostSignificantBits())
                        .putLong(id.getLeastSignificantBits())
                        .put(key)
                        .array();
            }

            signature.update(singable);
            return signature.verify(client.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
            if (!valid_warned) {
                valid_warned = true;
                plugin.console().send("Unfortunately, it seems that your server/vm is not compatible with the hash SHA1withRSA. Don't expect premium support", Level.WARNING);
            }

            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to validate client");
        }

        return false;
    }

    /**
     * Verify the message integrity and authenticity
     *
     * @param expected the expected message
     * @param decrypt the decrypt key
     * @param encrypt the encrypt key
     * @return if the message is valid
     */
    public static boolean verifyIntegrity(final byte[] expected, final PrivateKey decrypt, final byte[] encrypt) {
        try {
            Cipher cipher = Cipher.getInstance(decrypt.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, decrypt);
            byte[] decrypted = cipher.doFinal(encrypt);

            return Arrays.equals(expected, decrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            if (!nonce_warned) {
                nonce_warned = true;
                plugin.console().send("It seems that your server is not compatible with {0} hash. Don't expect premium support", Level.GRAVE, decrypt.getAlgorithm());
            }

            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to verify integrity of a message");
        }

        return false;
    }

    /**
     * Verify the message integrity and authenticity
     * which source is a client
     *
     * @param expected the expected message
     * @param client the client public key
     * @param salt the key salt
     * @param sign the signature
     * @return if the message is valid
     */
    public static boolean verifyClientIntegrity(final byte[] expected, final PublicKey client, final long salt, final byte[] sign) {
        try {
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(client);
            signature.update(expected);
            signature.update(Longs.toByteArray(salt));

            return signature.verify(sign);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
            if (!signed_nonce_warned) {
                signed_nonce_warned = true;
                plugin.console().send("It seems that your server is not compatible with SHA256WithRSA. Don't expect premium support", Level.GRAVE);
            }

            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to verify integrity of a signed message");
        }

        return false;
    }
}
