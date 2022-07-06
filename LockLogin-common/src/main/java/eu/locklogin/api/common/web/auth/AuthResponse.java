package eu.locklogin.api.common.web.auth;

public enum AuthResponse {
    SUCCESS,
    FAILED,
    NETWORK_ERROR;

    String message = "";

    public AuthResponse message(final String m) {
        message = m;

        return this;
    }

    public String getMessage() {
        return message;
    }
}
