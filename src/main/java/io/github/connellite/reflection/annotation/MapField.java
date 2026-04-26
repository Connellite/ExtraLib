package io.github.connellite.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures how a field is represented in object-to-map mapping.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MapField {

    /**
     * Output key for the mapped field. Blank value means field name.
     */
    String key() default "";

    /**
     * Excludes field from mapping when true.
     */
    boolean ignore() default false;
}
