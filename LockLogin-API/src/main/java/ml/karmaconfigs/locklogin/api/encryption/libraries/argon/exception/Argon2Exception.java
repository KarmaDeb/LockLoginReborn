package ml.karmaconfigs.locklogin.api.encryption.libraries.argon.exception;

/* dislike checked exceptions */
class Argon2Exception extends RuntimeException {
    Argon2Exception(String message) {
        super(message);
    }
}
