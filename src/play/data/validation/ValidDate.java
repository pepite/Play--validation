package play.data.validation;

import net.sf.oval.configuration.annotation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This field must be equals to another field.
 * Message key: validation.equals
 * $1: field name
 * $2: other field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = ValidDateCheck.class)
public @interface ValidDate {

    String message() default ValidDateCheck.mes;
    String[] value() default {""};
    String[] lang() default {"*"};

}

