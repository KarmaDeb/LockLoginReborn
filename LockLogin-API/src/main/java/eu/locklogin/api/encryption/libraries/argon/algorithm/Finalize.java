package eu.locklogin.api.encryption.libraries.argon.algorithm;

import eu.locklogin.api.encryption.libraries.argon.Argon2;
import eu.locklogin.api.encryption.libraries.argon.model.Block;
import eu.locklogin.api.encryption.libraries.argon.model.Instance;

public class Finalize {

    public static void finalize(Instance instance, Argon2 argon2) {

        Block finalBlock = instance.memory[instance.getLaneLength() - 1];

        /* XOR the last blocks */
        for (int i = 1; i < instance.getLanes(); i++) {
            int lastBlockInLane = i * instance.getLaneLength() + (instance.getLaneLength() - 1);
            finalBlock.xorWith(instance.memory[lastBlockInLane]);
        }

        byte[] finalBlockBytes = finalBlock.toBytes();
        byte[] finalResult = Functions.blake2bLong(finalBlockBytes, argon2.getOutputLength());

        argon2.setOutput(finalResult);

        if (argon2.isClearMemory()) {
            instance.clear();
            argon2.clear();
        }
    }
}
