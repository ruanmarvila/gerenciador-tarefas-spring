package dev.ruancmm.gerenciador_tarefas.core.validation;

import java.lang.reflect.Field;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOneFieldValidator implements ConstraintValidator<AtLeastOneField, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneField annotations) {
        this.fields = annotations.fields();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }

        for (String fieldName : fields) {
            try {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Object value = field.get(object);

                if (value != null) {
                    return true;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return false;
            }
        }
        return false;
    }
}
