package io.semval4j.jakarta;

import io.semval4j.core.exception.SemValException;
import io.semval4j.core.model.SemanticState;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts arbitrary validated objects (records or JavaBeans) into a
 * {@link SemanticState} using reflection, so no serialization framework leaks
 * into SemVal4j core.
 */
final class SemanticStateMapper {

    private SemanticStateMapper() {
    }

    static SemanticState toSemanticState(Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        Class<?> type = value.getClass();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (isIgnored(component)) {
                    continue;
                }
                values.put(component.getName(), read(component.getAccessor(), value));
            }
        } else {
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 0 || method.getReturnType() == void.class
                        || method.isAnnotationPresent(SemanticIgnore.class)) {
                    continue;
                }
                String property = propertyName(method);
                if (property != null) {
                    values.put(property, read(method, value));
                }
            }
        }
        return new SemanticState(values);
    }

    private static boolean isIgnored(RecordComponent component) {
        return component.isAnnotationPresent(SemanticIgnore.class)
                || component.getAccessor().isAnnotationPresent(SemanticIgnore.class);
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.equals("getClass")) {
            return null;
        }
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    private static String decapitalize(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static Object read(Method accessor, Object target) {
        try {
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new SemValException("Cannot read property via " + accessor.getName() + " on " + target.getClass().getName(), e);
        }
    }
}
