package eu.locklogin.plugin.bukkit.util.inventory.object;

import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.api.bukkit.server.BukkitServer;
import ml.karmaconfigs.api.bukkit.server.Version;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaKeyArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static eu.locklogin.plugin.bukkit.LockLogin.tryAsync;

/**
 * Private GSA code
 * <p>
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="https://karmaconfigs.ml/license/"> here </a>
 */
public class Button {

    private final static PropertyManager manager = new PropertyManager();
    private final static Map<Integer, ItemStack> numbers = new ConcurrentHashMap<>();

    private static KarmaMain translation;

    static {
        translation = new KarmaMain(plugin, "pin.kf").internal(PinInventory.class.getResourceAsStream("/templates/pin.kf"));

        try {
            translation.validate();
        } catch (Throwable ex) {
            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to validate pin translation file");

            plugin.console().send("Failed to validate pin translation file, default values may be used", Level.WARNING);
        }
    }

    public static void reload() {
        translation = new KarmaMain(plugin, "pin.kf").internal(PinInventory.class.getResourceAsStream("/templates/pin.kf"));

        try {
            translation.validate();
        } catch (Throwable ex) {
            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to validate pin translation file");

            plugin.console().send("Failed to validate pin translation file, default values may be used", Level.WARNING);
        }

        translation.clearCache();
        translation.preCache();
    }

    static Object getNumberProperties(int number) {
        String value;
        String signature;
        switch (number) {
            case 1:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDliMzAzMDNmOTRlN2M3ODVhMzFlNTcyN2E5MzgxNTM1ZGFmNDc1MzQ0OWVhNDFkYjc0NmUxMjM0ZTlkZDJiNSJ9fX0";
                signature = "ZZ8PHsFx2GMrlcm4rE1kgxiNE4RcDIWQ18vLQ+OYN9rb5eYZKlMDi8axbqtmB/sDalPvyOYuSR5njX+1qgwVfIFLO9tI9HkEqRROfsfMz74/e9t4HBwMvOyK65eDHjrQ6prIyssXGet8K41OMw94AN9om6nFX1hU4z0DqHx3+utGtkdnaTC5Fmi5Sse18vmSejY7GyfNJdwsayNBYcidJJgXYBHhvX6rtkd13DgAeX/ls0OdEDkWvjDJKEfJpHyXbzp7jRz+r4uqCk2ASGr1m2G9zlhE7yKiloC/P9n+24ks5E2VEqmeo9iDja8juQ2ynJLu0JalSeixtLFveFHkZn/kVs+9qQL9dsniLY2mAmtGKXZBdkWBoKiZxWqzh8fKowKKp/yT1u8DnDL1GpMXg33u8M+Urpoq/H9fLE+57z9PwrjEnytEUy5+QLvt6nZsIOaXKtfhseOlECslhP90ey4CWrukd4oBxz1C23QKk+a/Jy2TYnHDGUENKwBMvAShBLOnfpeugZQa0IZxB5XpraxTSWm18ODdfqnQIsqRU5wUo+m+7QGlXEpHUQQ3ZXexAnT073k0YqzfNjqye7irATZa4iogN+0zFU4OWCYXLCbGwu/tfeSqkS/g3vRb7nZ9P2TjIzPVPBfJgeu6C7Bkk6RJ0NgtSpvOxzgdlcbwUSU=";
                break;
            case 2:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTNjYTdkN2MxNTM0ZGM2YjllZDE2NDdmOTAyNWRkZjI0NGUwMTA3ZGM4ZGQ0ZjRmMDg1MmM4MjA4MWQ2MzUwZSJ9fX0=";
                signature = "HLLfBNdZg/MQdNEkNPNo8ubiSvmkOC9JDvISNIHG/LwFJVWmF+a/eDD2supazKDRWx1omt/b3+7T8K1qbLk6JwN2SNLBzSnYL+SpKn6csXNglnIltPSmc/u1HlheOuvrNUWcksMEWIUqVZWWvTBPNiFepbHkY29ErRaqEOX5fKzhGTyQhINSdXKf9bSwa8V/JJsCjSNGyznox/uVr81Ocm+5T+O5HlIqTjoVmIUKYrSQF+A2LPuvCx+u+8aZPMVTWs6ALgHto2saXTd4SbvmY8ocn6e1w+/Tbjf5+iiHsF48MC86J60xhamMg7Yzc/Ogs2cSACb6WOIuoSg3sDs5v9zXNf5PyvqrgOwzBwAfKwOP5OxA5p8mMGXGNPqncfwQBaY7jWKLagKEFP1HGprmhBQYfuo04fYc9Ccosw/5MYFb0XnMDo/HgRpxZD6/D3mk551MmuzhPdfwLrowLQmAU5Ps8PsitKghXR03vnOu4LH/rVN0b35cPDAwK+IYpqzzLBCpjgFU+V6QMmMiUvTmGeTeD0gRPo+7XhUxiEoXbbAzOGvnTZj/7lms+CF7F0T4++XXnTSksDTF5peOHfj0EI0jRtBzKYNq6Wv9pc3SbPa7jVGtZlD7Ju+xnjQn/ut//ItCvAo9lOvuNjvnX0G9aJpG73lJP8GwScSc2XyBe0c=";
                break;
            case 3:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTk1ZTFlMmZiMmRlN2U2Mjk5YTBmNjFkZGY3ZDlhNmQxMDFmOGQ2NjRmMTk1OWQzYjY3ZGNlOGIwNDlhOGFlMSJ9fX0=";
                signature = "mvBOdkvgZBA45ZGf1PivmXshmrcQBJaZsB0FN2/2ketLa10mmH6OFOt3hXZm2LmJs4OWDB69eBCl/QHT62JDf4u7v0Jh238nw3YOV5ieSTN+uxTqDoOM06pnpaPsoXYilUzJ26OwxO4hwKQRi7Fy4o+0Qh56N0QWLlGkH261KedCfu2oFUYmxD4qHq6m4Zd7Pjaepf7jJvIy9f+wqY8KqaGqx3/20hbOVCsWTbhnWLGa1/MPkWRdTCXiUrvoP2oCMSna73FtoPt4Lv1nSdhKM3iCchFJoS0NrqqROyzl3ONDflje2RwL3hVHSgWLN/V0BkWIlSwPnToGJEMZaVsSSLvXY/NETs1wMFDkl+y6Gz0Ldn6zCw1ofvXgC2eDBW3/IznJn2JN5gT/U0wrNqgO0U3WozW1H/HrfhBOT7y2Fu+iE6Gopj/9jcRwI8lvHW2rfpEfX/tZ5v7nqT5Lys57LYQeugbtED4b/2CcJbhmjOTuVZUJVfMaaq4J3ZKgUicEzfdsEp+1l5d0N9mk3lRI1UvqCzEE4MqZZJg4MU+CB/ILDSnFk1Xzny0MgHb5CgYz9xGDiVHKzkV0ZDc+PAs3b3WjZef1iedGJUoDkDk0FkXblacuR02ESgBNRsebIP72o/D+rRCyEMQwmCToJvczunTPYWqiUNyaeyE2gnsACTM=";
                break;
            case 4:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzgzOWVhZjllZTA3NjcwNjA3ZDNkYTRkNmMxZDMwZmU1OWRiNTY0NThmYWQ1ZjU1YjU0MTJkNTZiM2RlYjU1OSJ9fX0=";
                signature = "yWT4JVCSGpg3meT4YdTrN2A/AHkct1oQ8DIP/8mZI8EUjOuBhmEopwPrS8Aq2FpObC6kVfa6y0Hsl1y8MpdUYObyXaHTXbkH0t2O/lBw4RcH32vPkNEQR5yk2CF2B+VXRp92JWPsbtHpdpHJ/3mmTNycBo5KDDMOCjQnr5KIhECi8MZ7Yfay9r46GNzapnnz/EsVA3/4M7UoR8bm1D6lbrIiIzyT/MihFgV26JmXGk14wz+DVZnuXA3x4ZTtMww0A3eawtieND1b+3fe0sfWqTThENjD3ZBMzA5I4IfNtH/h1MQOmdZWnZe+H8Jbfd1zcPW9m8zmuGHyl4BAnfDmn9wfVEyVAhgRcr0/6JFCnQJHa9PURh5P3xFd/sR83UG1iI1es/aMPblSxeBfepjmYaRoU58ss3iy6n2t6GEIcB2cM8fPGtNVJL4ob9GMtjiJdQU9+xkXwaB9klEuObH5DDS0ERdJhCkbFakL/haIv/dawP60rpExKcTredsLaHhc0W5FWqasWL1MrvHEtgHWlrbD9NS4afhd2Vtrp1zpw2DHXez0T9ir4z+KVgjV+UYyhjJcBrY3MRAZoBhLOIu4p9iXvZhzvIvVvFXLC51V2HZwW8hFamMDcgtb/Rhh9MN5w2RtIZKurxNhRgyWOKDdFx64IOCswUZHF/MBSyBbKlI=";
                break;
            case 5:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgxNDBlYzQ4NDU2MzFhODlmZmU4MzQ0YWU5OGQxMDQ5YjgyYzIxNTkzOTMyOTBiZDM4YThhMDA4NDY1YjNkOSJ9fX0=";
                signature = "lBH0Kcpzs3xeGw/YOEeJtOmXqT4gaV9oc2vzhhGjfus9tn9nx/9yM/PMWBF6YCtYwJdjzhTdUEHXdbxGfNVA4G3GlirOlt8mvm6yhPCGxXqLeJO7BQvyVh8MAkzPPDpXr93WmQMo+nRB4rtw36lzlExGXUe4SFuzjU8DK1DnBePw7MNiFE9dxRi+S6W4nW7PBxdowH/fDrJOb8+RE8wLiTDZtfk5pPIEdjDcALxurq0CDANZ65Q6A2r787VoDoumlv2BQVAlg1Ny4BLs7OIzBdPy6Rdlq0Bqwv7zkP7GOvCbaaSznUZqEkH/25QQb1xt9FAtcmEY0dbtQ3z6J0jQJB5G/5RNogJj343BRBSYZ2yAYzAPe5CFGlshIQOmwHshLc5Fl0OKzXuDllUgaf5WD6wqYmMb0Cv8QWhS3Nchiun/TmH+npd812aJL7YK6S9yTe295qVpaCj9woSYoCpqLNQx36RjrmkgsZJjQCHoENd8degkaf8OcihMZN/balsNBiIbW3y7SbYoV2mw4yd1SpuHWchunopzd9EwSt/65hx5EFa40PvTD1+UI2zzR27/3S3RSo/mVDTxeK2SbqLTc5HIre1g2YbHfggsTLVNE8IeMEqUEkQb/wqRJZ14+82BjBZOoQ/d5fVwhzPI3ajvPCxg5f9bRzbB9tYByLY1Qn0=";
                break;
            case 6:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGViZjliNmE2YzRlYjBjYmZkMGEzZDM3YzM0YzQ1ODYwNjgxZjEzZjcyNzBmMTMzN2ZkMTM2YTcwMTE4OThmMCJ9fX0=";
                signature = "Q3yhhlIvleY02CUG99ptH5cl6hHC5ON3HkpGdQ6TOCAGGLuOObNIjrvSxzNEdhKGdpqZOH7YsY+gSw6s6WX/lxrkXWCZxFtfCJuwe4SujwEr7zP+KkE7JKbitFa07Y/yn6b3wFf6A+oRdKgxk1PslQAtRFzk/ghjk1H8Xu+AsWza9oWB6ljpyAsqnGZr3PBa/odwxfm9MalKUapdnsARpIhly/EeIhBAyODtkSxgvgwtp53CB7NglwSK3wmLlf1dFEUVAg3Oj+KUbc9cO+RFy5JO0zq90Ti1R5aTETv00gGd1Wf/0vQ7P5C3gNUpsgpYc9F7a+8dyhIua8GETWfHTvk9HfLBXy9R+IlBEYUDjm51Nh+lXm9Yx847LRISPJjPg332GjfF6o1M1ZNwlIuBqCpfeT/AAYnpvGm9ymtEPK3nCfbzLqJbMmcAGwdNoeW/smttLIiyzp7QlfFwoFUp/n/Ij5QetwGP/shwwIsY7No3XrZEuA9V1UNluB1oJFEU1m+J1k4z5m/I4zUxP8JS5KhHmUf4PiV4cdjcZmUzM60qCB5gckSBvYOVsv+/goZBMKILxo0Sb4jpqbih1/cYCJRzNufY1ttICR3srLoSNIuH1yOKcIdqeUitPo+g7C1RldMPtI3oryQzxMdPxehHX8a2FOVt+qMTfI1ybZnHYO8=";
                break;
            case 7:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRjMmQ2Y2JkZmYwMGI5N2FmN2Y4Y2ZlODc2N2ZkODdjZDY1NjM5YWJkZjgzZWMxMDM5YTQ2NDE1ZTY5ZTM5OCJ9fX0=";
                signature = "iU8r0GD4A86AipUORS6xYkmbmvvw7SlHD/qfT18BmRo7EvE/L23fg9IkcVA8OpNlr8qwB+RPQHrgWHVk0TzDWmuEILWG6hN5ni73rmCg5i0KbBVyg3xtkiqtwJpEhKV+ITtO+TTBydqGxj45SEUBJY5JWoGVtBBjGfvY8m2X6KQR8TtrOOAsQn8SK3nD+GbWUtJk00VDXC3FcEBIUDOF+Z9Jv0C5QRg1tAF2Mz7sVg8QmoKm29uDtdQ5eU5FsRHcPeik/kcT2NmypCZG3ntceumTJ+KDUldQXdoeksDZo6nOCdd43lIkrzQwQ1VoKvxr4QE6z0CZIHNEYrwBE+cDW5M2MrtRF7rPtAJKmE4yxbFrwhBZ/zp7l/bQE65RdZFulzi3qOzEJF5h42ivJRMowYMOA7c+d+oFiYf1uB2DaCfB1IPowjV9X4YyRWiPT0DupgcwOG8jqrJNLPMeVIiJJ5D4HihDGoYFuey+TMoA10fEgqr9Adv9cQD7AVsJ77ljkerTLvOT3SV7k0VfacAX7Cuoj8gAhy86B4shVLah8Fvc+grLZRJde+ce0T+c8f7U7YK67mmAvAQzOMEqz3v/+PioZoXAvlvd2JJMIOBYCVnOH1BxPWDWgNUqfazs4mme+pXYwxWdt8CPWo6OYdkp4S3QYUGRwQo1mlUoYZmlc90=";
                break;
            case 8:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQyNzkxNzUyZjY3YTY0NTBiOTc1ZDM1NzQxMjM1NmIwYTk5ZTM1NTVlNjdlYmRhZDJkNDAzMjYxMjliZGRhNCJ9fX0=";
                signature = "MHblF/1zYGGdyQ55K/ZLKPbkNKUduXhc4RyJzcDcxWSlOVTiU1H0UMHSIdBx6N54JIRNkKqV3jk+bAvT+ajfCQJRIAAHVelm6koXJ6MUEstCXR5f/XkiOrVKqy+aEM3ztJZnaxBTRUdpNNPfzAI03jvv0dPLlRojGQB2j1yUqWoAsMrYav3IvmJ6FaXq7gUUIwGtBHvV13rFab3lgCSQh/g9BKMwFzBxqHNSPgRVk8Bi9XT0E51z7/PmlR/C9goKvbvtNkaLtR9jht8y7fswv9FlVQDSjD9MLBBUGTW4sACFeTdejYoYO1l2Vsuc2DMXQE9GPA658IxBZNBCvOUkJtCD21JWBPCwgizQjvNybi6UZ5LFpXY5gQahxbRrU2eKdkmbgpJByB6iebVqEUdIy0MHl8rVhtPvZW4keqwkAlUcI8B747QTB46E74FKxwTrTAqpJYUzfRR0M3Lkvp0C9wPXbqra2OwQci2yUE5uZVYLVEWh73NX6Ybb5b8ehgKeMYVMG1ShRhFBV61TnVmwFIEi5rVezAwXZgQz+YFD40RThAVuC47QY19gedMhawtThcXlxGPMQvkHzi1OcrqaXv1k2nqASgbrEKwM1xpC5+VCjmbYOYihSulTUflDinmC+JHuy9OZe7JXZtIqagBDdZzbWZtuz80VNgOTKTcIhi8=";
                break;
            case 9:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTE3MDc3YzBjNjhjNDk2OTFhYTcwNDQzOTRhYWI5MDgzODYzOTRjM2Q0N2FhZmM1N2IwNmI4ZTMyZDE2NTZmNSJ9fX0=";
                signature = "HRbpYvdNz6ruk7H7Ci7GYZhUb99sgoTgXgXPHerugyGs0CsQcZ9MFgD8mRaVlMKTouRhaheMpueLC6KnM9Bvwaisctvj9ePi/YLwYB+vjSilUXrWMPMfgRcvvCJEaskh5nOyg2kalENtKtrGpR83i214BBL0au4FK2YHTSz1NphDclz3WF9vP8w+RRSxWdT1ZIB2/gDcwn2rOpsHLsWTaaaVCAdhpPpaRyPW8hNvTuFr1lq8Z9Mf1ua0znOCwPTZ7K/ACn7O9+vHg5GTEQ7tufWs2p9tnz7ozkW3cApYS/eX7PptIfTsL2x7VHqXd7z1tB4VIyln5ulMhVyeoi28mApvsKs4taB3yTqr8Ruc9yZm9EtYQT/eiJ8FNKtE2dY6ZIXHlJA1e+YHNGzM5jqVCCmM+XE0Jklz2ipdqmHhBkOVY1HsdtORsmKf/sFH65F7qdgYsum93HGoNdJhSKq9HdCWW817AiiXPKKaZwxH+m4aKOvnm5akTG3HOc+vdwiTVB4/dpXovObmeLYU0QQTMojD03TtYIQtcPw9oXd//Ztys/Lgnh/hJuyIMAAT6b61fsuuwopk7kRDAzDK0HyGsibOzfvlnt0TJVRaQAnVjKqJgoHBVOKF1cf+X38SlvcCbYnbp6amVkETO3wtcugu2LIdh4HLWzthOITqeXb5xKg=";
                break;
            case 0:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2I0NjhmNTU5OGFmN2M2NmYxYzFkNzM0NjVlYzMxZGNkNjdhODhkOTAwNTFiODk3ZGFkODRiYjM2MGIzMzc5OSJ9fX0=";
                signature = "T0txkzycIrkSfolqdwbdU7IQorbY8YYileCy61uV6UA1WrjZuD/o+Gy6duOdapR6hqA4YHFS8yambLHz6GavmRgsSQtP5zPXdUyPWrYhOQS1nUqtd3wOJ5Udt2YZDRmWGMUtLra0xNwhkoJRPvKH+WmJiTMjUnumwkauPKEoKNgVJMgwtxP4UAd/qq7wrkZfPuWFHNFwGYm5UQH+9/Xpq9TXvNxGcuSILuR1A+gLbd6Up68OGmEmO7XPpUf/hGgP2bpyyzD0pCr4fq0aCG/a5QfsWkFqQfVsmijzJYRpnhFzVfnsGooqrJ1I5sNgJN9ehJHAPaHo1t5I0igk2XQFyk1aZZ43jCOG3vgGWgeUSHAgcss9YrE09wsObRRltKKm6LtZ054vOYsrssOr2UWKll99Em93Gltv5QC3g+zh8tlrbMdFO+ATrhdOqB+JW1/ql47QUKuh6NaLmUqFr6Y7wUtpeeLKfQ30UOawg2czk2pUTYEiVIlKakvFdIsQEQG1iBV8VwnB7s3bqU6/94TWslo550BCTigr59QmdmYeOrTv+2sTssPD81cfrWyep7zsHsbOEkNbxATAIto2Tb5jIIS6SfxZXrQRlt5Mpj6aSFgC/ln54J20mYyzlvWq4VxA3Bjb95utnUQ6Lysd4w3k4L4KzH9de2384MHhQQAdV+0=";
                break;
            default:
                value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQxNTY0YmZlYzNiZDNhZjAzNTU4ODIzYzYwZTBlMTllODlhMGJjNzdjNjRmNWI3YjI1ZjVjNDM1YTViMWY5YyJ9fX0=";
                signature = "mvBOdkvgZBA45ZGf1PivmXshmrcQBJaZsB0FN2/2ketLa10mmH6OFOt3hXZm2LmJs4OWDB69eBCl/QHT62JDf4u7v0Jh238nw3YOV5ieSTN+uxTqDoOM06pnpaPsoXYilUzJ26OwxO4hwKQRi7Fy4o+0Qh56N0QWLlGkH261KedCfu2oFUYmxD4qHq6m4Zd7Pjaepf7jJvIy9f+wqY8KqaGqx3/20hbOVCsWTbhnWLGa1/MPkWRdTCXiUrvoP2oCMSna73FtoPt4Lv1nSdhKM3iCchFJoS0NrqqROyzl3ONDflje2RwL3hVHSgWLN/V0BkWIlSwPnToGJEMZaVsSSLvXY/NETs1wMFDkl+y6Gz0Ldn6zCw1ofvXgC2eDBW3/IznJn2JN5gT/U0wrNqgO0U3WozW1H/HrfhBOT7y2Fu+iE6Gopj/9jcRwI8lvHW2rfpEfX/tZ5v7nqT5Lys57LYQeugbtED4b/2CcJbhmjOTuVZUJVfMaaq4J3ZKgUicEzfdsEp+1l5d0N9mk3lRI1UvqCzEE4MqZZJg4MU+CB/ILDSnFk1Xzny0MgHb5CgYz9xGDiVHKzkV0ZDc+PAs3b3WjZef1iedGJUoDkDk0FkXblacuR02ESgBNRsebIP72o/D+rRCyEMQwmCToJvczunTPYWqiUNyaeyE2gnsACTM=";
                break;
        }

        return manager.createProperty("textures", value, signature);
    }

    public static ItemStack getNumber(final int number) {
        if (!numbers.containsKey(number)) {
            ItemStack stack = getRandomItemStack();

            ItemMeta stackMeta;
            if (BukkitServer.isOver(Version.v1_7_10)) {
                try {
                    stack = getSkull(getNumberProperties(number));
                } catch (Throwable ignore) {
                }

                stackMeta = stack.getItemMeta();
                assert stackMeta != null;
            } else {
                stack = new ItemStack(Material.getMaterial("SKULL_ITEM"), 1, (short) 3);
                stackMeta = stack.getItemMeta();


            }


            KarmaKeyArray def = new KarmaKeyArray();
            def.add("0", new KarmaObject("&b0"), false);
            def.add("1", new KarmaObject("&b1"), false);
            def.add("2", new KarmaObject("&b2"), false);
            def.add("3", new KarmaObject("&b3"), false);
            def.add("4", new KarmaObject("&b4"), false);
            def.add("5", new KarmaObject("&b5"), false);
            def.add("6", new KarmaObject("&b6"), false);
            def.add("7", new KarmaObject("&b7"), false);
            def.add("8", new KarmaObject("&b8"), false);
            def.add("9", new KarmaObject("&b9"), false);

            KarmaElement e = translation.get("numbers");
            if (e == null || !e.isKeyArray()) {
                e = def;
            }
            KarmaKeyArray k = (KarmaKeyArray) e;

            KarmaObject n;
            if (!k.containsKey(String.valueOf(number)) || !k.get(String.valueOf(number)).isString()) {
                n = new KarmaObject("&b" + number);
            } else {
                n = k.get(String.valueOf(number)).getObjet();
            }

            stackMeta.setDisplayName(StringUtils.toColor(n.getObjet().getString()));
            stack.setItemMeta(stackMeta);

            numbers.put(number, stack);
        }

        return numbers.get(number);
    }

    public static ItemStack confirm() {
        String value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJjMjI2MGI3MDI3YzM1NDg2NWU4M2MxMjlmMmQzMzJmZmViZGJiODVmNjY0NjliYmY5ZmQyMGYzYzNjNjA3NyJ9fX0=";
        String signature = "adDbjLBTJRxWiew0NFDsIsV0cMhFCKMsbhR7Y6vMu5qcp4k4Cp2cly7GA35Iu+C5X6zuBl5dEY7oONDsC3geVIihPA1bueUhy36uJVacT1iHsyJ6ipzqcJ/JJWYqW+9CQeMuIqYgds+0K9DokSDLUT7orPMTaFa0eegmALSvMgLwuuzyv/IhQvgUvRYydAQx8Gf8dnygHQmMJEUExWNaTVAlnQel01BWOa5LVtefx5pFygEgMCECFvxJbZ3y37uJFIKaLbVJDvN57bVOV9XeznTKpD5biytSn+zFH+a+DK8zboseAdL0UWzE1OBx9yf1xEG4+MZlb8Z4Jqn8vKsxf1cSArnjtQ2Rt7UbJGbTchqNN23SLcFy1MuFlNmRZVgNAj8vXsefENbX5Dp1rVNuolH8dbvhNnZN97bMchGX3SZeOLSXZuC8cXuj8sTAK9+mOT1okB3XMhN4Qy/NL48xRXWbgsZ9wUAYW8d7D+zKAmmGFVqb+QS5lZ6lAULoCTgvHkU7q37PKwLS+NN0aBJT9FSFqmGk5c6LFVJSWo/cfOwUn2lmEYZEfhBsM51wkoKFpgYhetsxSgplMEYCDNC35tlcUjYVzdpiW6urgDO+NcUMGvStVv9h6ymx71i1P2sUsBtq+XFjpmyNLxBX9OAUqzmgHE9ToV6kW3PogCmZhhk=";

        Object props = manager.createProperty("textures", value, signature);

        ItemStack stack = getRandomItemStack();
        try {
            stack = getSkull(props);
        } catch (Throwable ignore) {
        }
        ItemMeta stackMeta = stack.getItemMeta();
        assert stackMeta != null;

        KarmaElement confirm = translation.get("confirm");
        if (confirm == null || !confirm.isString()) {
            confirm = new KarmaObject("&aConfirm");
        }

        stackMeta.setDisplayName(StringUtils.toColor(confirm.getObjet().getString()));

        stack.setItemMeta(stackMeta);

        return stack;
    }

    public static ItemStack erase() {
        String value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzkzMTYxOGRlZTdkNGVjZGE5MWIxZDlmZDI3NDE1MzdjNmVkMjFlYTI2ZjcyYWEzZDk0MWUzOTE4NTM5MSJ9fX0=";
        String signature = "WnGvC/BfPe/OAACKKgLD+4JEO1r/o8BlALvNPWJYyf4UHoXlVhMv0BvWJOQOM1yYWdZGZ8PV7mU+RZgOB85t4hFJIT9v4hj/o8UBN3dYzPxlfZ3Bylbu3HNgVRrQT6BCiW+pq6svZLY6UEgu7oU2hvGQ68hY4M8W99WU04bHjfgzUYklsex80hbuBuTp1qvKWVeRBa9uiO6QHVXZq1wliRhuuw1oIM/Ndw6vtloyVyvzKjw9NKtIOVxl4wlNVW8FtQDscywcAMVz7OdjH5fgDWjHf08NHp183immzf50ZU8a8X2/wJYEmXABsPOcNpWW4xqOitqmGTGAhYG0B3z2bjgr3ae5qMB0Kf/4viAp9LaW3LuhJgZeRg42thzmmW8kbVvNiF8MLSKHKmLQez/mnxgiLooH4LG0Kpa0IrNlkOAEWW4pjtmnCHUtDQ++Xj8gG/jBsMZuU/7v3ozR+LtrIYyBQmnPl4dMexARwsplBHaT/Qk9v5mwd1Q7odMk6x5MgvcyAA8KcSgfOxUqjBMT6onS3GHswIqIk2xJLudknlnHXsJTByj8oJOEyY2xc/NBMjzu/0evM4fJdkbrogstgM1PvkR6U+qRuHEWuYcxftVoyxUywN9viCvMxfULBdLowWoMac4AB7UHjsJMSl/+sKgNPgIFJg9939y7MbCEV5I=";

        Object props = manager.createProperty("textures", value, signature);

        ItemStack stack = getRandomItemStack();
        try {
            stack = getSkull(props);
        } catch (Throwable ignore) {
        }
        ItemMeta stackMeta = stack.getItemMeta();
        assert stackMeta != null;

        KarmaElement erase = translation.get("erase");
        if (erase == null || !erase.isString()) {
            erase = new KarmaObject("&7Erase");
        }

        stackMeta.setDisplayName(StringUtils.toColor(erase.getObjet().getString()));

        stack.setItemMeta(stackMeta);

        return stack;
    }

    @SuppressWarnings("deprecation")
    public static ItemStack next() {
        ItemStack next;
        try {
            next = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1);
        } catch (Throwable e) {
            try {
                next = new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE", true)), 1, DyeColor.GREEN.getWoolData());
            } catch (Throwable ex) {
                next = new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE")), 1, DyeColor.GREEN.getWoolData());
            }
        }
        ItemMeta meta = next.getItemMeta();
        assert meta != null;

        PluginMessages messages = CurrentPlatform.getMessages();

        meta.setDisplayName(StringUtils.toColor(messages.nextButton()));
        next.setItemMeta(meta);

        return next;
    }

    @SuppressWarnings("deprecation")
    public static ItemStack back() {
        ItemStack back;
        try {
            back = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        } catch (Throwable e) {
            try {
                back = new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE", true)), 1, DyeColor.RED.getWoolData());
            } catch (Throwable ex) {
                back = new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE")), 1, DyeColor.RED.getWoolData());
            }
        }
        ItemMeta meta = back.getItemMeta();
        assert meta != null;

        PluginMessages messages = CurrentPlatform.getMessages();

        meta.setDisplayName(StringUtils.toColor(messages.backButton()));
        back.setItemMeta(meta);

        return back;
    }

    /**
     * Get a skull by it's properties
     *
     * @param properties the properties
     * @return an ItemStack
     */
    @SuppressWarnings("deprecation")
    static ItemStack getSkull(final Object properties) {
        ItemStack skull = getRandomItemStack();

        try {
            ItemStack head;
            try {
                head = new ItemStack(Material.PLAYER_HEAD, 1);
            } catch (Throwable e) {
                try {
                    head = new ItemStack(Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM", true)), 1, (short) 3);
                } catch (Throwable ex) {
                    head = new ItemStack(Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")), 1, (short) 3);
                }
            }

            SkullMeta headMeta = (SkullMeta) head.getItemMeta();

            Class<?> gameProfile = null;
            try {
                gameProfile = Class.forName("com.mojang.authlib.GameProfile");
            } catch (Throwable ex) {
                try {
                    gameProfile = Class.forName("net.minecraft.util.com.mojang.authlib.GameProfile"); //mojang, wtf?
                } catch (Throwable ignored) {}
            }

            if (gameProfile != null) {
                Constructor<?> profileConstructor = gameProfile.getConstructor(UUID.class, String.class);
                Object profile = profileConstructor.newInstance(UUID.randomUUID(), null);

                Object propmap = ReflectionUtil.invokeMethod(gameProfile, profile, "getProperties");
                ReflectionUtil.invokeMethod(propmap, "clear");
                ReflectionUtil.invokeMethod(propmap.getClass(), propmap, "put", new Class[]{Object.class, Object.class}, "textures", properties);

                Field profileField;
                assert headMeta != null;
                profileField = headMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(headMeta, profile);
                head.setItemMeta(headMeta);
                return head;
            } else {
                plugin.console().send("Failed to create textured skull");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return skull;
    }

    /**
     * Get a random item stack
     *
     * @return an item stack
     */
    public static ItemStack getRandomItemStack() {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (Material material : Material.values()) {
            try {
                stacks.add(new ItemStack(material, 1));
            } catch (Throwable ignore) {
            }
        }
        int random = new Random().nextInt(stacks.size() - 1);
        if (random == 0) random = 1;
        return stacks.get(random);
    }

    /**
     * Make the server pre-load the cache
     */
    @SuppressWarnings("deprecation")
    public static void preCache() {
        tryAsync(TaskTarget.CACHE, () -> {
            for (int i = 0; i < 9; i++)
                getNumber(i);

            confirm();
            erase();
            next();
            back();

            try {
                new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE", true)), 1, (short) 3);
                new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE")), 1, (short) 3);
                new ItemStack(Material.LEGACY_STAINED_GLASS_PANE, 1, (short) 3);
            } catch (Throwable ignored) {
            }
        });
    }

    /**
     * Possible enumeration
     */
    public enum Action {
        /**
         * Click '1' number stack
         */
        ONE(1),

        /**
         * Click '2' number stack
         */
        TWO(2),

        /**
         * Click '3' number stack
         */
        THREE(3),

        /**
         * Click '4' number stack
         */
        FOUR(4),

        /**
         * Click '5' number stack
         */
        FIVE(5),

        /**
         * Click '6' number stack
         */
        SIX(6),

        /**
         * Click '7' number stack
         */
        SEVEN(7),

        /**
         * Click '8' number stack
         */
        EIGHT(8),

        /**
         * Click '9' number stack
         */
        NINE(9),

        /**
         * Click '0' number stack
         */
        ZERO(0),

        /**
         * Erase number input
         */
        ERASE(Integer.MIN_VALUE),

        /**
         * Confirm pin input
         */
        CONFIRM(Integer.MAX_VALUE),

        /**
         * Next page
         */
        NEXT(50),

        /**
         * Previous page
         */
        BACK(-50),

        /**
         * Prevent errors
         */
        NONE(-1);

        private final int number;

        Action(final int n) {
            number = n;
        }

        /**
         * Check if the clicked item is similar
         * to the one to check with
         *
         * @param clicked the clicked item
         * @param check   the one to check with
         * @return if the clicked stack is similar to the "check" one
         */
        private static boolean isSimilar(ItemStack clicked, ItemStack check) {
            boolean isSimilar = false;
            if (clicked.hasItemMeta()) {
                assert clicked.getItemMeta() != null;
                if (clicked.getItemMeta().hasDisplayName()) {
                    if (check.hasItemMeta()) {
                        assert check.getItemMeta() != null;
                        if (check.getItemMeta().hasDisplayName()) {
                            if (StringUtils.stripColor(clicked.getItemMeta().getDisplayName()).equals(StringUtils.stripColor(check.getItemMeta().getDisplayName()))) {
                                isSimilar = true;
                            }
                        }
                    }
                }
            }

            return isSimilar;
        }

        /**
         * Get the action attached to the item stack
         *
         * @param stack the item
         * @return the action attached to the item
         */
        public static Action getAction(final ItemStack stack) {
            for (int i = 0; i <= 9; i++) {
                ItemStack n = Button.getNumber(i);
                if (isSimilar(stack, n)) {
                    return getByNumber(i);
                }
            }

            ItemStack erase = erase();
            ItemStack confirm = confirm();
            ItemStack next = next();
            ItemStack back = back();
            if (isSimilar(stack, erase)) {
                return Action.ERASE;
            }

            if (isSimilar(stack, confirm)) {
                return Action.CONFIRM;
            }

            if (isSimilar(stack, next)) {
                return Action.NEXT;
            }

            if (isSimilar(stack, back)) {
                return Action.BACK;
            }

            return Action.NONE;
        }

        /**
         * Get an action by its id
         *
         * @param n the action id
         * @return the action
         */
        public static Action getByNumber(final int n) {
            switch (n) {
                case 0:
                    return Action.ZERO;
                case 1:
                    return Action.ONE;
                case 2:
                    return Action.TWO;
                case 3:
                    return Action.THREE;
                case 4:
                    return Action.FOUR;
                case 5:
                    return Action.FIVE;
                case 6:
                    return Action.SIX;
                case 7:
                    return Action.SEVEN;
                case 8:
                    return Action.EIGHT;
                case 9:
                    return Action.NINE;
                case Integer.MIN_VALUE:
                    return Action.ERASE;
                case Integer.MAX_VALUE:
                    return Action.CONFIRM;
                case 50:
                    return Action.NEXT;
                case -50:
                    return Action.BACK;
                case -1:
                default:
                    return Action.NONE;
            }
        }

        /**
         * Get the number attached to the action
         *
         * @return the action number
         */
        public int getNumber() {
            return number;
        }

        /**
         * Get the friendly name of the action, in case
         * the action was a number click, the number will
         * be returned instead
         *
         * @return the friendly action name
         */
        public String friendly() {
            switch (this) {
                case ONE:
                    return "1";
                case TWO:
                    return "2";
                case THREE:
                    return "3";
                case FOUR:
                    return "4";
                case FIVE:
                    return "5";
                case SIX:
                    return "6";
                case SEVEN:
                    return "7";
                case EIGHT:
                    return "8";
                case NINE:
                    return "9";
                case ZERO:
                    return "0";
                case ERASE:
                    return "erase";
                case CONFIRM:
                    return "confirm";
                case NEXT:
                    return "->";
                case BACK:
                    return "<-";
                case NONE:
                default:
                    return "";
            }
        }
    }
}
