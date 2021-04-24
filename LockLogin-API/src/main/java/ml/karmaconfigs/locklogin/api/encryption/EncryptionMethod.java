package ml.karmaconfigs.locklogin.api.encryption;

public abstract class EncryptionMethod {

    public abstract String hash();
    public abstract boolean check();
}
