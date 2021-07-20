package eu.locklogin.api.encryption.libraries.argon.algorithm;

import eu.locklogin.api.encryption.libraries.argon.Argon2;
import eu.locklogin.api.encryption.libraries.argon.Constants;
import eu.locklogin.api.encryption.libraries.argon.Util;
import eu.locklogin.api.encryption.libraries.argon.model.Instance;

public class Initialize {


    public static void initialize(Instance instance, Argon2 argon2) {
        byte[] initialHash = Functions.initialHash(
                Util.intToLittleEndianBytes(argon2.getLanes()),
                Util.intToLittleEndianBytes(argon2.getOutputLength()),
                Util.intToLittleEndianBytes(argon2.getMemory()),
                Util.intToLittleEndianBytes(argon2.getIterations()),
                Util.intToLittleEndianBytes(argon2.getVersion()),
                Util.intToLittleEndianBytes(argon2.getType().ordinal()),
                Util.intToLittleEndianBytes(argon2.getPasswordLength()),
                argon2.getPassword(),
                Util.intToLittleEndianBytes(argon2.getSaltLength()),
                argon2.getSalt(),
                Util.intToLittleEndianBytes(argon2.getSecretLength()),
                argon2.getSecret(),
                Util.intToLittleEndianBytes(argon2.getAdditionalLength()),
                argon2.getAdditional()
        );
        fillFirstBlocks(instance, initialHash);
    }

    /**
     * (H0 || 0 || i) 72 byte -> 1024 byte
     * (H0 || 1 || i) 72 byte -> 1024 byte
     */
    private static void fillFirstBlocks(Instance instance, byte[] initialHash) {

        final byte[] zeroBytes = {0, 0, 0, 0};
        final byte[] oneBytes = {1, 0, 0, 0};

        byte[] initialHashWithZeros = getInitialHashLong(initialHash, zeroBytes);
        byte[] initialHashWithOnes = getInitialHashLong(initialHash, oneBytes);

        for (int i = 0; i < instance.getLanes(); i++) {

            byte[] iBytes = Util.intToLittleEndianBytes(i);

            System.arraycopy(iBytes, 0, initialHashWithZeros, Constants.ARGON2_PREHASH_DIGEST_LENGTH + 4, 4);
            System.arraycopy(iBytes, 0, initialHashWithOnes, Constants.ARGON2_PREHASH_DIGEST_LENGTH + 4, 4);

            byte[] blockhashBytes = Functions.blake2bLong(initialHashWithZeros, Constants.ARGON2_BLOCK_SIZE);
            instance.memory[i * instance.getLaneLength()].fromBytes(blockhashBytes);

            blockhashBytes = Functions.blake2bLong(initialHashWithOnes, Constants.ARGON2_BLOCK_SIZE);
            instance.memory[i * instance.getLaneLength() + 1].fromBytes(blockhashBytes);
        }
    }

    private static byte[] getInitialHashLong(byte[] initialHash, byte[] appendix) {
        byte[] initialHashLong = new byte[Constants.ARGON2_PREHASH_SEED_LENGTH];

        System.arraycopy(initialHash, 0, initialHashLong, 0, Constants.ARGON2_PREHASH_DIGEST_LENGTH);
        System.arraycopy(appendix, 0, initialHashLong, Constants.ARGON2_PREHASH_DIGEST_LENGTH, 4);

        return initialHashLong;
    }

}
