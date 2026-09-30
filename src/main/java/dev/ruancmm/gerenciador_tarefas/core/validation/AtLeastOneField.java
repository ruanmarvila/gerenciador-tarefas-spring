package dev.ruancmm.gerenciador_tarefas.core.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = AtLeastOneFieldValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneField {

    String message() default "At least one field must be provided";
    String[] fields();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
