package eu.nk2.intercom;

import java.lang.reflect.Array;

//
// Shoutout: https://stackoverflow.com/questions/180097/dynamically-find-the-class-that-represents-a-primitive-java-type
//
public class ClassUtils {
    public static <C> Class<C[]> arrayClass(Class<C> klass) {
        return (Class<C[]>) Array.newInstance(klass, 0).getClass();
    }

    public static Class<?> objectiveClass(Class<?> klass) {
        Class<?> component = klass.getComponentType();
        if (component != null) {
            if (component.isPrimitive() || component.isArray())
                return ClassUtils.arrayClass(ClassUtils.objectiveClass(component));
        } else if (klass.isPrimitive()) {
            if (klass == char.class)
                return Character.class;
            if (klass == int.class)
                return Integer.class;
            if (klass == boolean.class)
                return Boolean.class;
            if (klass == byte.class)
                return Byte.class;
            if (klass == double.class)
                return Double.class;
            if (klass == float.class)
                return Float.class;
            if (klass == long.class)
                return Long.class;
            if (klass == short.class)
                return Short.class;
        }

        return klass;
    }

    public static Class<?> primitiveClass(Class<?> klass) {
        if (klass == Character.class)
            return char.class;
        if (klass == Integer.class)
            return int.class;
        if (klass == Boolean.class)
            return boolean.class;
        if (klass == Byte.class)
            return byte.class;
        if (klass == Double.class)
            return double.class;
        if (klass == Float.class)
            return float.class;
        if (klass == Long.class)
            return long.class;
        if (klass == Short.class)
            return short.class;

        throw new IllegalArgumentException(klass + " don't have a primitive type");
    }
}
