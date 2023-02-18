package eu.locklogin.api.common.security.backup.data;

import eu.locklogin.api.security.backup.data.LockAccountBackup;
import lombok.AllArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
public class JsonLockAccount implements LockAccountBackup {

    private final String issuer;
    private final long creation;

    /**
     * Get the lock issuer
     *
     * @return the issuer
     */
    @Override
    public String issuer() {
        return issuer;
    }

    /**
     * Get the lock date
     *
     * @return the lock date
     */
    @Override
    public Instant date() {
        return Instant.ofEpochMilli(creation);
    }
}
