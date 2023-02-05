package ml;

import com.google.common.hash.Hashing;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.encryption.Validation;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.*;

public class Main {

    private final static KarmaSource source = new KarmaSource() {
        @Override
        public String name() {
            return "LockLogin";
        }

        @Override
        public String version() {
            return null;
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public String[] authors() {
            return new String[0];
        }

        @Override
        public String updateURL() {
            return null;
        }
    };

    public static void main(String[] args) throws IOException {

    }
}
