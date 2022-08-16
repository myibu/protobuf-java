package com.github.myibu.proto;

import java.lang.reflect.*;
import java.util.*;

/**
 * JavaBean
 *
 * @author hdh
 * Created on 2022/8/12
 */
public class JavaBean {
    public static Object getFieldValue(Field field, Object value) {
        Class<?> declaringClass = field.getDeclaringClass();
        Class<?> fieldType = field.getType();
        Method[] setterAndGetterMethod = findSetterAndGetterMethod(field, declaringClass, fieldType);
        Object fieldValue;
        try {
            fieldValue = setterAndGetterMethod[1].invoke(value);
        } catch (Exception e) {
            throw new ProtoProcessingException("invoke " + declaringClass.getSimpleName() + "'s " + setterAndGetterMethod[1].getName() + " method failed", e);
        }
        return fieldValue;
    }

    public static void setFieldValue(Field field, Object value, Object fieldValue) {
        Class<?> declaringClass = field.getDeclaringClass();
        Class<?> fieldType = field.getType();
        Method[] setterAndGetterMethod = findSetterAndGetterMethod(field, declaringClass, fieldType);
        try {
            setterAndGetterMethod[0].invoke(value, fieldValue);
        } catch (Exception e) {
            throw new ProtoProcessingException("invoke " + declaringClass.getSimpleName() + "'s " + setterAndGetterMethod[0].getName() + " method failed", e);
        }
    }

    private static Method[] findSetterAndGetterMethod(Field field, Class<?> declaringClass, Class<?> fieldType) {
        Method[] methods = new Method[2];
        String setterMethodName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        try {
            Method setterMethod = declaringClass.getDeclaredMethod(setterMethodName, fieldType);
            methods[0] = setterMethod;
        } catch (NoSuchMethodException e) {
            throw new ProtoProcessingException(declaringClass.getSimpleName() + "'s " + setterMethodName + " method not found", e);
        }

        String getterMethodName;
        if (boolean.class == fieldType) {
            getterMethodName = "is" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        } else {
            getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        }
        try {
            Method getterMethod = declaringClass.getDeclaredMethod(getterMethodName);
            methods[1] = getterMethod;
        } catch (NoSuchMethodException e) {
            throw new ProtoProcessingException(declaringClass.getSimpleName() + "'s " + getterMethodName + " method not found", e);
        }
        return methods;
    }

    public static Object newInstanceByNoArgsConstructor(Class<?> type) {
        Object value;
        try {
            Constructor<?> defaultConstructor = type.getDeclaredConstructor();
            value = defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new ProtoProcessingException("default constructor of " + type.getSimpleName() + " not found", e);
        } catch (Exception e) {
            throw new ProtoProcessingException("invoke " + type.getSimpleName() + "'s default constructor failed", e);
        }
        return value;
    }

    public static Class<?> getListGenericType(Field field) {
        return getGenericType(field, 0);
    }

    public static Class<?> getMapGenericType(Field field, boolean isKey) {
        return isKey ? getGenericType(field, 0) : getGenericType(field, 1);
    }

    private static Class<?> getGenericType(Field field, int index) {
        Class<?> repeatedType = null;
        Type genericType = field.getGenericType();
        if(genericType instanceof ParameterizedType){
            ParameterizedType pt = (ParameterizedType) genericType;
            if (index >= 0 && index < pt.getActualTypeArguments().length) {
                repeatedType = (Class<?>)pt.getActualTypeArguments()[index];
            }
        }
        if (repeatedType == null) {
            throw new ProtoProcessingException("'" + field.getName() + "' must has generic type");
        }
        return repeatedType;
    }

    public static Class<?> getListOrArrayRepeatedType(Object value) {
        Class<?> valueType = value.getClass();
        if (valueType.isArray()) {
            return valueType.getComponentType();
        }
        // if value is list, size must > 0
        Iterator<?> listIterator = ((Iterable<?>) value).iterator();
        if (listIterator.hasNext()) {
            Object firstRepeated = listIterator.next();
            return firstRepeated.getClass();
        }
        throw new ProtoProcessingException("can not decide repeated type for " + valueType.getSimpleName());
    }

    public static Collection<Object> getFieldCollectionValueOrCreate(Field field, Object value, boolean usePacked) {
        Collection<Object> collection = null;
        Class<?> fieldType = field.getType();
        if (List.class == fieldType) {
            if (usePacked) {
                collection = new ArrayList<>();
            } else {
                if ((collection = (Collection<Object>)getFieldValue(field, value)) == null) {
                    collection = new ArrayList<>();
                }
            }
        } else if (Set.class == fieldType) {
            if (usePacked) {
                collection = new HashSet<>();
            } else {
                if ((collection = (Collection<Object>)getFieldValue(field, value)) == null) {
                    collection = new HashSet<>();
                }
            }
        } else if (Queue.class == fieldType) {
            if (usePacked) {
                collection = new ArrayDeque<>();
            } else {
                if ((collection = (Collection<Object>)getFieldValue(field, value)) == null) {
                    collection = new ArrayDeque<>();
                }
            }
        } else {
            boolean isAbstract = Modifier.isAbstract(fieldType.getModifiers());
            boolean isInterface = Modifier.isInterface(fieldType.getModifiers());
            if (isAbstract || isInterface) {
                throw new ProtoProcessingException("'" + field.getName() + "' type can not be abstract or interface collection");
            }
            try {
                if (usePacked) {
                    Constructor<?> noArgConstructor = fieldType.getConstructor();
                    collection = (Collection<Object>) noArgConstructor.newInstance();
                } else {
                    if ((collection = (Collection<Object>)getFieldValue(field, value)) == null) {
                        Constructor<?> noArgConstructor = fieldType.getConstructor();
                        collection = (Collection<Object>) noArgConstructor.newInstance();
                    }
                }
            } catch (NoSuchMethodException e) {
                throw new ProtoProcessingException("default constructor of " + fieldType.getSimpleName() + " not found", e);
            } catch (Exception e) {
                throw new ProtoProcessingException("invoke " + fieldType.getSimpleName() + "'s default constructor failed", e);
            }
        }
        return collection;
    }

    public static Map<Object, Object> getFieldMapValueOrCreate(Field field, Object value) {
        Map<Object, Object> map = null;
        Class<?> fieldType = field.getType();
        if (Map.class == fieldType) {
            if ((map = (Map<Object, Object>)getFieldValue(field, value)) == null) {
                map = new HashMap<>();
            }
        } else {
            boolean isAbstract = Modifier.isAbstract(fieldType.getModifiers());
            boolean isInterface = Modifier.isInterface(fieldType.getModifiers());
            if (isAbstract || isInterface) {
                throw new ProtoProcessingException("'" + field.getName() + "' type can not be abstract or interface map");
            }
            try {
                if ((map = (Map<Object, Object>)getFieldValue(field, value)) == null) {
                    Constructor<?> noArgConstructor = fieldType.getConstructor();
                    map = (Map<Object, Object>) noArgConstructor.newInstance();
                }
            } catch (NoSuchMethodException e) {
                throw new ProtoProcessingException("default constructor of " + fieldType.getSimpleName() + " not found", e);
            } catch (Exception e) {
                throw new ProtoProcessingException("invoke " + fieldType.getSimpleName() + "'s default constructor failed", e);
            }
        }
        return map;
    }

    public static Object[] getArrayPackagedValue(Object value, Class<?> repeatedType) {
        // convert basic data type to Object
        Object[] array = new Object[0];
        if (byte.class == repeatedType) {
            byte[] arrayValue = (byte[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else if (short.class == repeatedType) {
            int[] arrayValue = (int[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else if (int.class == repeatedType) {
            int[] arrayValue = (int[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else if (long.class == repeatedType) {
            long[] arrayValue = (long[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        }  else if (float.class == repeatedType) {
            float[] arrayValue = (float[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else if (double.class == repeatedType) {
            double[] arrayValue = (double[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else if (boolean.class == repeatedType) {
            boolean[] arrayValue = (boolean[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        }  else if (char.class == repeatedType) {
            char[] arrayValue = (char[])value;
            array = new Object[arrayValue.length];
            for (int i = 0; i < arrayValue.length; i++) {
                array[i] = arrayValue[i];
            }
        } else {
            array = (Object[]) value;
        }
        return array;
    }
}
