package eu.locklogin.plugin.bukkit.util.inventory.object;

import li.cock.ie.reflect.DuckBypass;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Private GSA code
 * <p>
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="https://karmaconfigs.ml/license/"> here </a>
 */
public class ReflectionUtil {

    private static final DuckBypass reflect = new DuckBypass();
    public static String serverVersion = null;

    static {
        try {
            Class.forName("org.bukkit.Bukkit");
            String version = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf(46) + 1);
            setObject(ReflectionUtil.class, null, "serverVersion", version);
        } catch (Exception var1) {
        }

    }

    public static Class<?> getBukkitClass(String clazz) throws Exception {
        return Class.forName("org.bukkit.craftbukkit." + serverVersion + "." + clazz);
    }

    public static Class<?> getBungeeClass(String path, String clazz) throws Exception {
        return Class.forName("net.md_5.bungee." + path + "." + clazz);
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) throws Exception {
        Constructor<?> c = clazz.getConstructor(args);
        c.setAccessible(true);
        return c;
    }

    public static Enum<?> getEnum(Class<?> clazz, String constant) throws Exception {
        Class<?> c = Class.forName(clazz.getName());
        Enum<?>[] econstants = (Enum[]) c.getEnumConstants();
        Enum[] var4 = econstants;
        int var5 = econstants.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            Enum<?> e = var4[var6];
            if (e.name().equalsIgnoreCase(constant)) {
                return e;
            }
        }

        throw new Exception("Enum constant not found " + constant);
    }

    public static Enum<?> getEnum(Class<?> clazz, String enumname, String constant) throws Exception {
        Class<?> c = Class.forName(clazz.getName() + "$" + enumname);
        Enum<?>[] econstants = (Enum[]) c.getEnumConstants();
        Enum[] var5 = econstants;
        int var6 = econstants.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            Enum<?> e = var5[var7];
            if (e.name().equalsIgnoreCase(constant)) {
                return e;
            }
        }

        throw new Exception("Enum constant not found " + constant);
    }

    private static Field getField(Class<?> clazz, String fname) throws Exception {
        Field f;
        try {
            f = clazz.getDeclaredField(fname);
        } catch (Exception var4) {
            f = clazz.getField(fname);
        }

        setFieldAccessible(f);
        return f;
    }

    public static Object getFirstObject(Class<?> clazz, Class<?> objclass, Object instance) throws Exception {
        Field f = null;
        Field[] var4 = clazz.getDeclaredFields();
        int var5 = var4.length;

        int var6;
        Field fi;
        for (var6 = 0; var6 < var5; ++var6) {
            fi = var4[var6];
            if (fi.getType().equals(objclass)) {
                f = fi;
                break;
            }
        }

        if (f == null) {
            var4 = clazz.getFields();
            var5 = var4.length;

            for (var6 = 0; var6 < var5; ++var6) {
                fi = var4[var6];
                if (fi.getType().equals(objclass)) {
                    f = fi;
                    break;
                }
            }
        }

        assert f != null;

        setFieldAccessible(f);
        return f.get(instance);
    }

    private static Method getMethod(Class<?> clazz, String mname) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(mname);
        } catch (Exception var6) {
            try {
                m = clazz.getMethod(mname);
            } catch (Exception var5) {
                return null;
            }
        }

        m.setAccessible(true);
        return m;
    }

    public static <T> Field getField(Class<?> target, String name, Class<T> fieldType, int index) {
        Field[] var4 = target.getDeclaredFields();
        int var5 = var4.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            Field field = var4[var6];
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0) {
                field.setAccessible(true);
                return field;
            }
        }

        if (target.getSuperclass() != null) {
            return getField(target.getSuperclass(), name, fieldType, index);
        } else {
            throw new IllegalArgumentException("Cannot find field with type " + fieldType);
        }
    }

    private static Method getMethod(Class<?> clazz, String mname, Class<?>... args) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(mname, args);
        } catch (Exception var7) {
            try {
                m = clazz.getMethod(mname, args);
            } catch (Exception var6) {
                return null;
            }
        }

        m.setAccessible(true);
        return m;
    }

    public static Class<?> getNMSClass(String clazz) throws Exception {
        return Class.forName("net.minecraft.server." + serverVersion + "." + clazz);
    }

    public static Object getObject(Class<?> clazz, Object obj, String fname) throws Exception {
        return getField(clazz, fname).get(obj);
    }

    public static Object getObject(Object obj, String fname) throws Exception {
        return getField(obj.getClass(), fname).get(obj);
    }

    public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initargs) throws Exception {
        return getConstructor(clazz, args).newInstance(initargs);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws Exception {
        return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initargs) throws Exception {
        return Objects.requireNonNull(getMethod(clazz, method, args)).invoke(obj, initargs);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, String string) throws InvocationTargetException, IllegalAccessException {
        return Objects.requireNonNull(getMethod(clazz, method, args)).invoke(obj, string);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Object... initargs) throws Exception {
        return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj, initargs);
    }

    public static Object invokeMethod(Object obj, String method) throws Exception {
        return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj);
    }

    public static Object invokeMethod(Object obj, String method, Object[] initargs) throws Exception {
        return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj, initargs);
    }

    private static void setFieldAccessible(Field f) throws Exception {
        reflect.setEditable(f);
    }

    public static void setObject(Class<?> clazz, Object obj, String fname, Object value) throws Exception {
        reflect.setValue(clazz, fname, obj, value);
    }

    public static void setObject(Object obj, String fname, Object value) throws Exception {
        getField(obj.getClass(), fname).set(obj, value);
    }
}
