package ml.karmaconfigs.locklogin.api.encryption.argon.util.exception;

/* dislike checked exceptions */
class Argon2Exception extends RuntimeException {
    Argon2Exception(String message) {
        super(message);
    }
}
