package ml.karmaconfigs.locklogin.manager.bungee.manager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */
public class Reflections {

    /**
     * Set a method accessible and return the same method
     *
     * @param t   the method
     * @param <T> the method type
     * @return the accessible method
     */
    public static <T extends java.lang.reflect.AccessibleObject> T setAccessible(T t) {
        t.setAccessible(true);
        return t;
    }

    /**
     * Get a field value
     *
     * @param obj       the class object
     * @param fieldname the field name
     * @param <T>       the field type
     * @return the field value
     * @throws IllegalAccessException if the field is private or package-private
     * @throws NoSuchFieldException   if the field does not exist
     */
    public static <T> T getFieldValue(Object obj, String fieldname) throws IllegalAccessException, NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        while (true) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldname))
                    return (T) setAccessible(clazz.getDeclaredField(fieldname)).get(obj);
            }
            if ((clazz = clazz.getSuperclass()) == null)
                throw new NoSuchFieldException("Can't find field " + fieldname);
        }
    }

    /**
     * Get a field value
     *
     * @param clazz     the class
     * @param fieldname the field name
     * @param <T>       the field type
     * @return the field value
     * @throws IllegalAccessException if the field is private or package-private
     * @throws NoSuchFieldException   if the field does not exist
     */
    public static <T> T getStaticFieldValue(Class<?> clazz, String fieldname) throws IllegalAccessException, NoSuchFieldException {
        while (true) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldname))
                    return (T) setAccessible(clazz.getDeclaredField(fieldname)).get(null);
            }
            if ((clazz = clazz.getSuperclass()) == null)
                throw new NoSuchFieldException("Can't find field " + fieldname);
        }
    }

    /**
     * Set a field value
     *
     * @param obj       the class object
     * @param fieldname the field name
     * @throws IllegalAccessException if the field is private or package-private
     * @throws NoSuchFieldException   if the field does not exist
     */
    public static void setFieldValue(Object obj, String fieldname, Object value) throws IllegalAccessException, NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        while (true) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldname)) {
                    setAccessible(clazz.getDeclaredField(fieldname)).set(obj, value);
                    return;
                }
            }
            if ((clazz = clazz.getSuperclass()) == null)
                throw new NoSuchFieldException("Can't find field " + fieldname);
        }
    }

    /**
     * Invoke a class method
     *
     * @param obj        the class object
     * @param methodname the method name
     * @param args       the method arguments
     * @throws IllegalAccessException    if the field is private or package-private
     * @throws InvocationTargetException if something goes wrong while calling method
     */
    public static void invokeMethod(Object obj, String methodname, Object... args) throws IllegalAccessException, InvocationTargetException {
        Class<?> clazz = obj.getClass();
        do {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodname) && (method.getParameterTypes()).length == args.length)
                    setAccessible(method).invoke(obj, args);
            }
        } while ((clazz = clazz.getSuperclass()) != null);
    }
}
