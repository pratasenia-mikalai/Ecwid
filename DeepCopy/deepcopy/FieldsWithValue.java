package deepcopy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;

/**
 *
 * Iterable wrapper to iterate through non-static fields with non-null values of object including superclass fields
 *
 */
public class FieldsWithValue implements Iterable<Field> {

    private final Object object;

    public FieldsWithValue(Object object) {
        this.object = object;
    }

    @Override
    public Iterator<Field> iterator() {
        return new ValueFieldIterator(object);
    }

    private static class ValueFieldIterator implements Iterator<Field> {

        private final Object object;
        private Class<?> currentClass;
        private Field[] currentClassFields;
        private int nextFieldIndex = -1;
        private Field nextField = null;

        private ValueFieldIterator(Object object) {
            this.object = object;
            this.currentClass = object.getClass();
            this.currentClassFields = object.getClass().getDeclaredFields();
            lookForNextField();
        }

        @Override
        public boolean hasNext() {
            return nextField != null;
        }

        @Override
        public Field next() {
            Field field = nextField;
            if (field != null) {
                lookForNextField();
            }
            return field;
        }

        private void lookForNextField() {
            while (currentClass != Object.class) {
                for(int i = nextFieldIndex + 1; i < currentClassFields.length; i++) {
                    Field field = currentClassFields[i];
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        if (field.get(object) != null) {
                            nextField = field;
                            nextFieldIndex = i;
                            return;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                stateToSuperclass();
            }
            this.nextField = null;
        }

        private void stateToSuperclass() {
            currentClass = currentClass.getSuperclass();
            currentClassFields = currentClass.getDeclaredFields();
            nextFieldIndex = -1;
        }
    }
}