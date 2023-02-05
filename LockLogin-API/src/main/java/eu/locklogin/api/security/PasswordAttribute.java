package eu.locklogin.api.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PasswordAttribute {

    @Getter
    private final int length;
    @Getter
    private final int specialCharacters;
    @Getter
    private final int numbers;
    @Getter
    private final int upperCase;
    @Getter
    private final int lowerCase;
}
