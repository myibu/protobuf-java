package com.github.myibu.proto;

import com.github.myibu.proto.annotation.FieldNumber;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ProtoDef
 *
 * @author hdh
 * Created on 2022/8/12
 */
public interface ProtoDef {
    // write_type
    int WRITE_TYPE_VAR_INT = 0;
    int WRITE_TYPE_64_BIT = 1;
    int WRITE_TYPE_LENGTH_DELIMITED_BIT = 2;
    @Deprecated
    int WRITE_TYPE_START_GROUP_BIT = 3;
    @Deprecated
    int WRITE_TYPE_END_GROUP_BIT = 4;
    int WRITE_TYPE_32_BIT = 5;

    int KEY_TAG = 3;
    int VAR_INT_GROUP_LENGTH = 7;

    // item.key.fieldNumber = 1, item.key.fieldNumber = 2
    int MAP_KEY_FIELD_NUMBER = 1;
    int MAP_VALUE_FIELD_NUMBER = 2;

    // default value
    enum DefaultValue {
        STRINGS(aClass -> String.class == aClass, ""::equals, valueType -> ""),
        BYTES(aClass -> aClass.isArray() && aClass.getComponentType() == byte.class, aValue -> Arrays.equals(new byte[0], (byte[]) aValue), valueType -> new byte[0]),
        BOOLS(aClass -> Boolean.class == aClass, Boolean.FALSE::equals, valueType -> false),
        NUMERIC(aClass -> Integer.class == aClass || Long.class == aClass || Float.class == aClass || Double.class == aClass, aValue -> {
            if (Integer.class == aValue.getClass()) {
                return 0 == (int) aValue;
            }
            if (Long.class == aValue.getClass()) {
                return 0 == (long) aValue;
            }
            if (Float.class == aValue.getClass()) {
                return Float.compare(0, (float) aValue) == 0;
            }
            if (Double.class == aValue.getClass()) {
                return Double.compare(0, (double) aValue) == 0;
            }
            return false;
        }, valueType -> 0),
        ENUMS(Class::isEnum, aValue -> aValue.getClass().getEnumConstants()[0] == aValue, valueType -> valueType.getEnumConstants()[0]),
        ;
        private final Predicate<Class<?>> classPredicate;
        private final Predicate<Object> defaultValuePredicate;
        private final Function<Class<?>, Object> defaultValueFunction;

        DefaultValue(Predicate<Class<?>> classPredicate, Predicate<Object> defaultValuePredicate, Function<Class<?>, Object> defaultValueFunction) {
            this.classPredicate = classPredicate;
            this.defaultValuePredicate = defaultValuePredicate;
            this.defaultValueFunction = defaultValueFunction;
        }

        public static boolean anyMatch(Object value) {
            for (DefaultValue dv : DefaultValue.values()) {
                if (dv.classPredicate.test(value.getClass()) && dv.defaultValuePredicate.test(value)) {
                    return true;
                }
            }
            return false;
        }

        public static Object createForType(Class<?> valueType) {
            for (DefaultValue dv : DefaultValue.values()) {
                if (dv.classPredicate.test(valueType)) {
                    return dv.defaultValueFunction.apply(valueType);
                }
            }
            return null;
        }
    }

    class FieldNumberInfo {
        private final int value;
        private final ProtoType type;

        public FieldNumberInfo(int value, ProtoType type) {
            this.value = value;
            this.type = type;
        }

        public FieldNumberInfo(FieldNumber fieldNumber) {
            this(fieldNumber.value(), fieldNumber.type());
        }

        public FieldNumberInfo(int value) {
            this(value, ProtoType.UNDEFINED);
        }

        public int value() {
            return this.value;
        }

        public ProtoType type() {
            return this.type;
        }
    }

    default boolean usePackedForType(Class<?> repeatedType) {
        boolean usePacked = false;
        if (Integer.class == repeatedType || int.class == repeatedType
                || Long.class == repeatedType || long.class == repeatedType
                || Boolean.class == repeatedType || boolean.class == repeatedType
                || repeatedType.isEnum()) {
            usePacked = true;
        } else if (Double.class == repeatedType || double.class == repeatedType) {
            usePacked = true;
        } else if (Float.class == repeatedType || float.class == repeatedType) {
            usePacked = true;
        }
        return usePacked;
    }


    default byte[] writeObjectAsBytes(Object value) {
        return new byte[0];
    }

    default <T> T readObject(byte[] encodedBytes, Class<T> valueType) {
        return null;
    }
}
