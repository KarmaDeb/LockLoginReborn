package eu.locklogin.api.common.web.editor.cookie;

import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.apache.http.cookie.Cookie;

import java.time.Instant;
import java.util.Date;

public final class EditorCookie implements Cookie {

    private final String path, name, value, domain, info, info_url;
    private final int version;
    private final boolean persistent, secure;
    private final int[] ports;

    private Date expiration;

    /**
     * Initialize the editor cookies
     *
     * @param pA the cookie path
     * @param n the cookie name
     * @param v the cookie value
     * @param d the cookie domain
     * @param i the cookie info
     * @param iU the cookie info URL
     * @param cV the cookie version
     * @param p if the cookie is persistent
     * @param s if the cookie is secure
     * @param e the cookie expiration date
     * @param pS the cookie ports
     */
    public EditorCookie(final String pA, final String n, final String v, final String d, final String i, final String iU, final int cV, final boolean p, final boolean s, final Date e, final int... pS) {
        path = pA;
        name = n;
        value = v;
        domain = d;
        info = i;
        info_url = iU;
        version = cV;
        persistent = p;
        secure = s;
        expiration = e;
        ports = pS;
    }

    /**
     * Returns the name.
     *
     * @return String name The name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     *
     * @return String value The current value.
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * Returns the comment describing the purpose of this cookie, or
     * {@code null} if no such comment has been defined.
     *
     * @return comment
     */
    @Override
    public String getComment() {
        return info;
    }

    /**
     * If a user agent (web browser) presents this cookie to a user, the
     * cookie's purpose will be described by the information at this URL.
     */
    @Override
    public String getCommentURL() {
        return info_url;
    }

    /**
     * Returns the expiration {@link Date} of the cookie, or {@code null}
     * if none exists.
     * <p><strong>Note:</strong> the object returned by this method is
     * considered immutable. Changing it (e.g. using setTime()) could result
     * in undefined behaviour. Do so at your peril. </p>
     *
     * @return Expiration {@link Date}, or {@code null}.
     */
    @Override
    public Date getExpiryDate() {
        return expiration;
    }

    /**
     * Returns {@code false} if the cookie should be discarded at the end
     * of the "session"; {@code true} otherwise.
     *
     * @return {@code false} if the cookie should be discarded at the end
     * of the "session"; {@code true} otherwise
     */
    @Override
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Returns domain attribute of the cookie. The value of the Domain
     * attribute specifies the domain for which the cookie is valid.
     *
     * @return the value of the domain attribute.
     */
    @Override
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the path attribute of the cookie. The value of the Path
     * attribute specifies the subset of URLs on the origin server to which
     * this cookie applies.
     *
     * @return The value of the path attribute.
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * Get the Port attribute. It restricts the ports to which a cookie
     * may be returned in a Cookie request header.
     */
    @Override
    public int[] getPorts() {
        return ports;
    }

    /**
     * Indicates whether this cookie requires a secure connection.
     *
     * @return {@code true} if this cookie should only be sent
     * over secure connections, {@code false} otherwise.
     */
    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * Returns the version of the cookie specification to which this
     * cookie conforms.
     *
     * @return the version of the cookie.
     */
    @Override
    public int getVersion() {
        return version;
    }

    /**
     * Returns true if this cookie has expired.
     *
     * @param date Current time
     * @return {@code true} if the cookie has expired.
     */
    @Override
    public boolean isExpired(final Date date) {
        if (persistent)
            return false;

        Instant check = date.toInstant();
        Instant expiry = expiration.toInstant();

        System.out.println(expiry.isAfter(check));
        System.out.println(check.toEpochMilli() == expiry.toEpochMilli());
        return expiry.isAfter(check) || check.toEpochMilli() == expiry.toEpochMilli();
    }

    /**
     * Update the cookie expiration date
     *
     * @param newExpiration the new expiration date
     */
    public void setExpiration(final Date newExpiration) {
        expiration = newExpiration;
    }

    /**
     * Transform a cookie into a editor cookie
     *
     * @param cookie the cookie
     * @return the cookie as an editor cookie
     */
    public static EditorCookie fromCookie(final Cookie cookie) {
        if (cookie instanceof EditorCookie) {
            System.out.println("Already editor cookie");
            return (EditorCookie) cookie;
        } else {
            String path = cookie.getPath();
            String name = cookie.getName();
            String value = cookie.getValue();
            String domain = cookie.getDomain();
            String info = cookie.getComment();
            String info_url = cookie.getCommentURL();

            int version = cookie.getVersion();

            boolean persistent = cookie.isPersistent();
            boolean secure = cookie.isSecure();

            System.out.println("Persistent: " + persistent);

            int[] ports = new int[0];
            if (cookie.getPorts() != null)
                ports = cookie.getPorts();

            if (StringUtils.isNullOrEmpty(path))
                path = "/";
            if (StringUtils.isNullOrEmpty(name))
                name = "undefined";
            if (StringUtils.isNullOrEmpty(value))
                value = "undefined";
            if (StringUtils.isNullOrEmpty(domain))
                domain = "localhost";
            if (StringUtils.isNullOrEmpty(info))
                info = "";
            if (StringUtils.isNullOrEmpty(info_url))
                info_url = "localhost";

            Date expiration = cookie.getExpiryDate();
            if (expiration == null) {
                expiration = new Date();
                persistent = true;
                System.out.println("Expiration date is null");
            }

            return new EditorCookie(
                    path,
                    name,
                    value,
                    domain,
                    info,
                    info_url,
                    version,
                    persistent,
                    secure,
                    expiration,
                    ports
            );
        }
    }
}
