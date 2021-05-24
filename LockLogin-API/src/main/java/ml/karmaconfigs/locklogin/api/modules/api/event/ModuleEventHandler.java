package ml.karmaconfigs.locklogin.api.modules.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation must be used over a event
 * method to make the plugin know it's a event
 * handler method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ModuleEventHandler {

    Priority priority() default Priority.NORMAL;
    String after() default "";

    /**
     * Event listener priority
     */
    enum Priority {
        FIRST, NORMAL, LAST, AFTER
    }
}
