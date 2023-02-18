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

package eu.locklogin.plugin.bukkit.premium;

import com.github.games647.craftapi.model.skin.SkinProperty;
import eu.locklogin.plugin.bukkit.premium.mojang.client.ClientKey;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Part of the code of this class is from:
 * <a href="https://github.com/games647/FastLogin/blob/main/bukkit/src/main/java/com/github/games647/fastlogin/bukkit/BukkitLoginSession.java">FastLogin</a>
 */
public final class LoginSession {

    @Getter
    private final String username;
    @Getter
    @Setter
    private UUID id;
    private final static byte[] EMPTY = {};
    @Getter
    private final byte[] token;
    @Getter
    private final ClientKey key;

    @Getter
    @Setter
    private boolean verified;
    @Getter
    @Setter
    private SkinProperty skin;

    public LoginSession(final String name, final byte[] token, final ClientKey key) {
        username = name;
        this.token = (token != null ? token : EMPTY);
        this.key = key;
    }
}
