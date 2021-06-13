package eu.locklogin.api.encryption.libraries.argon;

import eu.locklogin.api.encryption.libraries.argon.model.Argon2Type;

import java.util.Arrays;
import java.util.Base64;

public final class Argon2Result {

    private final Argon2Type type;
    private final int version;
    private final int memoryInKb;
    private final int iteration;
    private final int paralellism;
    private final byte[] salt;
    private final byte[] hash;

    Argon2Result(Argon2Type type, int version, int memoryInKiB, int iteration, int parallelism, byte[] salt, byte[] hash) {
        this.type = type;
        this.version = version;
        this.memoryInKb = memoryInKiB;
        this.iteration = iteration;
        this.paralellism = parallelism;
        this.salt = Arrays.copyOf(salt, salt.length);
        this.hash = Arrays.copyOf(hash, hash.length);
    }

    public byte[] asByte() {
        return Arrays.copyOf(hash, hash.length);
    }

    public String asString() {
        return Util.bytesToHexString(hash);
    }

    public String getEncoded() {
        String type = this.type.equals(Argon2Type.Argon2i) ? "i" :
                this.type.equals(Argon2Type.Argon2d) ? "d" :
                        this.type.equals(Argon2Type.Argon2id) ? "id" : null;
        String salt = Base64.getEncoder().withoutPadding().encodeToString(this.salt);
        String hash = Base64.getEncoder().withoutPadding().encodeToString(this.hash);

        return "$argon2" +
                type +
                "$v=" +
                version +
                "$m=" +
                memoryInKb +
                ",t=" +
                iteration +
                ",p=" +
                paralellism +
                "$" +
                salt +
                "$" +
                hash;
    }
}
