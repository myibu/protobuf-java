package com.github.myibu.proto;

import com.github.myibu.algorithm.data.Bits;
import com.github.myibu.algorithm.data.Bytes;
import com.github.myibu.proto.annotation.FieldNumber;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.myibu.proto.JavaBean.*;

/**
 * ProtoReader
 *
 * @author hdh
 * Created on 2022/8/12
 */
public class ProtoReader implements ProtoDef {
    @Override
    public <T> T readObject(byte[] encodedBytes, Class<T> valueType) {
        return readObjectValue(encodedBytes, valueType);
    }

    public <T> T readObjectValue(byte[] encodedBytes, Class<T> valueType) {
        Object value = newInstanceByNoArgsConstructorWithDefaultValue(valueType);
        return (T) readObjectValueThenSet(encodedBytes, 0, encodedBytes.length, valueType, value).value;
    }

    private static Object newInstanceByNoArgsConstructorWithDefaultValue(Class<?> valueType) {
        Object value = newInstanceByNoArgsConstructor(valueType);
        Class<?> targetClass = value.getClass();
        List<Field> validFields = Arrays.stream(targetClass.getDeclaredFields())
                .filter(field -> null != field.getAnnotation(FieldNumber.class) && field.getAnnotation(FieldNumber.class).useDefaultValue())
                .collect(Collectors.toList());
        for (Field validField: validFields) {
            Object fieldValue = getFieldValue(validField, value);
            if (fieldValue == null) {
                fieldValue = DefaultValue.createForType(validField.getType());
                setFieldValue(validField, value, fieldValue);
            }
        }
        return value;
    }

    private static class KeyInfo {
        int fieldNumber;
        int writeType;
        int keyLength;

        KeyInfo(int fieldNumber, int writeType, int keyLength) {
            this.fieldNumber = fieldNumber;
            this.writeType = writeType;
            this.keyLength = keyLength;
        }
    }

    private static class ValueInfo {
        Object value;
        int valueLength;

        ValueInfo(Object value, int valueLength) {
            this.value = value;
            this.valueLength = valueLength;
        }
    }

    private KeyInfo readKey(byte[] encodedBytes, int start) {
        byte[] keyBytes = readVarIntBytes(encodedBytes, start);
        int keyV = (int)varIntBytesToObject(keyBytes, Integer.class, null);
        // write_type: 低3位
        int writeType = keyV & 0x07;
        int fieldNumber = (keyV ^ writeType) >> KEY_TAG;
        return new KeyInfo(fieldNumber, writeType, keyBytes.length);
    }

    private ValueInfo readVarIntValueThenSet(byte[] encodedBytes, int start, Field field, Object value) {
        byte[] valueBytes = readVarIntBytes(encodedBytes, start);
        Object fieldValue = varIntBytesToObject(valueBytes, field.getType(), new FieldNumberInfo(field.getAnnotation(FieldNumber.class)));
        setFieldValue(field, value, fieldValue);
        return new ValueInfo(fieldValue, valueBytes.length);
    }

    private ValueInfo read64BitValueThenSet(byte[] encodedBytes, int start, Field field, Object value) {
        double fieldValue = read64BitValue(Arrays.copyOfRange(encodedBytes, start, start + 8));
        setFieldValue(field, value, fieldValue);
        return new ValueInfo(fieldValue, 8);
    }

    private ValueInfo readLengthDelimitedValueThenSet(byte[] encodedBytes, int start, int end, Field field, Object value) {
        Class<?> fieldType = field.getType();
        byte[] embeddedBytes = readVarIntBytes(encodedBytes, start);
        int embeddedLength = (int)varIntBytesToObject(embeddedBytes, Integer.class, new FieldNumberInfo(field.getAnnotation(FieldNumber.class)));
        // strings
        if (fieldType == String.class) {
            byte[] stringBytes = Arrays.copyOfRange(encodedBytes, start + embeddedBytes.length, start + embeddedBytes.length + embeddedLength);
            String stringValue = new String(stringBytes);
            setFieldValue(field, value, stringValue);
            return new ValueInfo(stringValue, embeddedBytes.length + stringBytes.length);
        }
        // packed repeated fields
        else if (Iterable.class.isAssignableFrom(fieldType)) {
            Class<?> repeatedType = getListGenericType(field);
            boolean usePacked = usePackedForType(repeatedType);
            // collection
            if (Collection.class.isAssignableFrom(fieldType)) {
                Collection<Object> list = getFieldCollectionValueOrCreate(field, value, usePacked);
                // repeated fields of scalar numeric types use packed encoding by default.
                if (usePacked) {
                    start = start + embeddedBytes.length;
                    int i = 0;
                    while (i < embeddedLength) {
                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        list.add(itemValueInfo.value);
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, list);
                    return new ValueInfo(list, embeddedBytes.length + embeddedLength);
                } else {
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    list.add(itemValueInfo.value);
                    setFieldValue(field, value, list);
                    return new ValueInfo(list, itemValueInfo.valueLength);
                }
            } else {
                throw new ProtoProcessingException("'" + field.getName() + "' with " + field.getType().getSimpleName() + " type is not support");
            }
        }
        // array
        else if (fieldType.isArray()) {
            Class<?> repeatedType = fieldType.getComponentType();
            // read byes like string
            if (byte.class == repeatedType || Byte.class == repeatedType) {
                byte[] bytes = Arrays.copyOfRange(encodedBytes, start + embeddedBytes.length, start + embeddedBytes.length + embeddedLength);
                setFieldValue(field, value, bytes);
                return new ValueInfo(bytes, embeddedBytes.length + bytes.length);
            }
            boolean usePacked = usePackedForType(repeatedType);
            // repeated fields of scalar numeric types use packed encoding by default.
            if (usePacked) {
                start = start + embeddedBytes.length;
                if (int.class == repeatedType) {
                    int[] arrayValue = new int[0];
                    int i = 0;
                    while (i < embeddedLength) {
                        int[] newArray = new int[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;

                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = (int)itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                } else if (long.class == repeatedType) {
                    long[] arrayValue = new long[embeddedLength];
                    int i = 0;
                    while (i < embeddedLength) {
                        long[] newArray = new long[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;
                        
                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = (long)itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                } else if (float.class == repeatedType) {
                    float[] arrayValue = new float[embeddedLength];
                    int i = 0;
                    while (i < embeddedLength) {
                        float[] newArray = new float[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;

                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = (float)itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                } else if (double.class == repeatedType) {
                    double[] arrayValue = new double[embeddedLength];
                    int i = 0;
                    while (i < embeddedLength) {
                        double[] newArray = new double[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;

                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = (double)itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                } else if (boolean.class == repeatedType) {
                    boolean[] arrayValue = new boolean[embeddedLength];
                    int i = 0;
                    while (i < embeddedLength) {
                        boolean[] newArray = new boolean[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;

                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = (boolean)itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                } else {
                    Object[] arrayValue = new Object[embeddedLength];
                    int i = 0;
                    while (i < embeddedLength) {
                        Object[] newArray = new Object[arrayValue.length+1];
                        System.arraycopy(arrayValue, 0, newArray, 0, arrayValue.length);
                        arrayValue = newArray;

                        ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start + i, repeatedType);
                        arrayValue[arrayValue.length-1] = itemValueInfo.value;
                        i = i + itemValueInfo.valueLength;
                    }
                    setFieldValue(field, value, arrayValue);
                    return new ValueInfo(arrayValue, embeddedBytes.length + embeddedLength);
                }
            } else {
                if (int.class == repeatedType) {
                    int[] array = (int[])getFieldValue(field, value);
                    if (array == null) {
                        array = new int[1];
                    } else {
                        int[] newArray = new int[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = (int)itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                } else if (long.class == repeatedType) {
                    long[] array = (long[])getFieldValue(field, value);
                    if (array == null) {
                        array = new long[1];
                    } else {
                        long[] newArray = new long[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = (long)itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                } else if (float.class == repeatedType) {
                    float[] array = (float[])getFieldValue(field, value);
                    if (array == null) {
                        array = new float[1];
                    } else {
                        float[] newArray = new float[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = (float)itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                } else if (double.class == repeatedType) {
                    double[] array = (double[])getFieldValue(field, value);
                    if (array == null) {
                        array = new double[1];
                    } else {
                        double[] newArray = new double[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = (double)itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                } else if (boolean.class == repeatedType) {
                    boolean[] array = (boolean[])getFieldValue(field, value);
                    if (array == null) {
                        array = new boolean[1];
                    } else {
                        boolean[] newArray = new boolean[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = (boolean)itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                } else {
                    Object[] array = (Object[])getFieldValue(field, value);
                    if (array == null) {
                        array = new Object[1];
                    } else {
                        Object[] newArray = new Object[array.length+1];
                        System.arraycopy(array, 0, newArray, 0, array.length);
                        array = newArray;
                    }
                    ValueInfo itemValueInfo = readRepeatedBytes(encodedBytes, start, repeatedType);
                    array[array.length-1] = itemValueInfo.value;
                    setFieldValue(field, value, array);
                    return new ValueInfo(array, itemValueInfo.valueLength);
                }
            }
        }
        // map
        else if (Map.class.isAssignableFrom(fieldType)) {
            Map<Object, Object> map = getFieldMapValueOrCreate(field, value);
            Class<?> mapKeyType = getMapGenericType(field, true);
            Class<?> mapValueType = getMapGenericType(field, false);

            start = start + embeddedBytes.length;
            ValueInfo mapItemKey = readMapKeyOrValueBytes(encodedBytes, start, mapKeyType);
            ValueInfo mapItemValue = readMapKeyOrValueBytes(encodedBytes, start + mapItemKey.valueLength, mapValueType);
            map.put(mapItemKey.value, mapItemValue.value);
            setFieldValue(field, value, map);
            return new ValueInfo(map, embeddedBytes.length + mapItemKey.valueLength + mapItemValue.valueLength);
        }
        // embedded messages
        else {
            Object fieldValue = newInstanceByNoArgsConstructorWithDefaultValue(fieldType);
            ValueInfo embeddedObjectInfo = readObjectValueThenSet(encodedBytes, start + embeddedBytes.length, end, fieldType, fieldValue);
            setFieldValue(field, value, embeddedObjectInfo.value);
            return embeddedObjectInfo;
        }
    }

    private ValueInfo readObjectValueThenSet(byte[] encodedBytes, int offset, int end, Class<?> valueType, Object value) {
        int start = offset;
        while (offset < end) {
            KeyInfo keyInfo = readKey(encodedBytes, offset);
            Optional<Field> validFieldOpl = Arrays.stream(valueType.getDeclaredFields())
                    .filter(field -> null != field.getAnnotation(FieldNumber.class) && keyInfo.fieldNumber == field.getAnnotation(FieldNumber.class).value())
                    .findFirst();
            offset += keyInfo.keyLength;
            if (validFieldOpl.isPresent()) {
                // write_type=0
                if (keyInfo.writeType == WRITE_TYPE_VAR_INT) {
                    ValueInfo valueInfo = readVarIntValueThenSet(encodedBytes, offset, validFieldOpl.get(), value);
                    offset += valueInfo.valueLength;
                }
                // write_type=1
                else if (keyInfo.writeType == WRITE_TYPE_64_BIT) {
                    ValueInfo valueInfo = read64BitValueThenSet(encodedBytes, offset, validFieldOpl.get(), value);
                    offset += valueInfo.valueLength;
                }
                // write_type=2
                else if (keyInfo.writeType == WRITE_TYPE_LENGTH_DELIMITED_BIT) {
                    ValueInfo valueInfo = readLengthDelimitedValueThenSet(encodedBytes, offset, end, validFieldOpl.get(), value);
                    offset += valueInfo.valueLength;
                }
                // write_type=5
                else if (keyInfo.writeType == WRITE_TYPE_32_BIT) {
                    ValueInfo valueInfo = read32BitValueThenSet(encodedBytes, offset, validFieldOpl.get(), value);
                    offset += valueInfo.valueLength;
                }
            } else {
                break;
            }
        }
        return new ValueInfo(value, offset - start);
    }

    private ValueInfo readStartGroupValueThenSet(byte[] encodedBytes, int start, Field field, Object value){
        throw new UnsupportedOperationException("not support write_type=3");
    }

    private ValueInfo readEndGroupValueThenSet(byte[] encodedBytes, int start, Field field, Object value){
        throw new UnsupportedOperationException("not support write_type=4");
    }

    private ValueInfo read32BitValueThenSet(byte[] encodedBytes, int start, Field field, Object value){
        float fieldValue = read32BitValue(Arrays.copyOfRange(encodedBytes, start, start + 4));
        setFieldValue(field, value, fieldValue);
        return new ValueInfo(fieldValue, 4);
    }

    private ValueInfo readRepeatedBytes(byte[] encodedBytes, int start, Class<?> fieldType) {
        // write_type=0
        if (Integer.class == fieldType || int.class == fieldType ||
                Long.class == fieldType  || long.class == fieldType ||
                Boolean.class == fieldType || boolean.class == fieldType ||
                fieldType.isEnum()) {
            byte[] varIntBytes = readVarIntBytes(encodedBytes, start);
            Object fieldValue = varIntBytesToObject(varIntBytes, Integer.class, null);
            return new ValueInfo(fieldValue, varIntBytes.length);
        }
        // write_type=1
        else if (Double.class == fieldType || double.class == fieldType) {
            return read64BitValue(encodedBytes, start, fieldType);
        }
        // write_type=2
        else if (String.class == fieldType) {
            byte[] embeddedBytes0 = readVarIntBytes(encodedBytes, start);
            int embeddedLength0 = (int)varIntBytesToObject(embeddedBytes0, Integer.class, null);
            byte[] stringBytes = Arrays.copyOfRange(encodedBytes, start + embeddedBytes0.length, start + embeddedBytes0.length + embeddedLength0);
            String stringValue = new String(stringBytes);
            return new ValueInfo(stringValue, embeddedBytes0.length + embeddedLength0);
        }
        // write_type=5
        else if (Float.class == fieldType || float.class == fieldType) {
            return read32BitValue(encodedBytes, start, fieldType);
        }
        else {
            byte[] objectLengthBytes = readVarIntBytes(encodedBytes, start);
            int objectLength = (int)varIntBytesToObject(objectLengthBytes, Integer.class, null);
            ValueInfo objectValueInfo = readObjectValue(encodedBytes, start + objectLengthBytes.length, start + objectLengthBytes.length + objectLength, fieldType);
            return new ValueInfo(objectValueInfo.value, objectLengthBytes.length + objectValueInfo.valueLength);
        }
    }

//    private byte[] readMapKeyOrValueLength(byte[] encodedBytes, int start) {
//        byte[] length = readVarIntBytes(encodedBytes, start);
//        byte[] fieldNumber = readVarIntBytes(encodedBytes, start + length.length);
//        return Bytes.appendBytes(length, fieldNumber);
//    }

    private byte[] readMapKeyOrValueLength(byte[] encodedBytes, int start) {
        return readVarIntBytes(encodedBytes, start);
    }

    private byte[] readKeyOrValueFieldNumber(byte[] encodedBytes, int start) {
        return readVarIntBytes(encodedBytes, start);
    }

    private ValueInfo readMapKeyOrValueBytes(byte[] encodedBytes, int start, Class<?> fieldType) {
        // write_type=0
        if (Integer.class == fieldType || Long.class == fieldType || Boolean.class == fieldType || fieldType.isEnum()) {
            byte[] fieldNumber = readKeyOrValueFieldNumber(encodedBytes, start);
            byte[] varIntBytes = readVarIntBytes(encodedBytes, start + fieldNumber.length);
            Object fieldValue = varIntBytesToObject(varIntBytes, Integer.class, null);
            return new ValueInfo(fieldValue, fieldNumber.length + varIntBytes.length);
        }
        // write_type=1
        else if (Double.class == fieldType) {
            byte[] fieldNumber = readKeyOrValueFieldNumber(encodedBytes, start);
            ValueInfo valueInfo = read64BitValue(encodedBytes, start + fieldNumber.length, fieldType);
            return new ValueInfo(valueInfo.value, fieldNumber.length + valueInfo.valueLength);
        }
        // write_type=2
        else if (String.class == fieldType) {
            byte[] fieldNumber = readKeyOrValueFieldNumber(encodedBytes, start);
            byte[] embeddedBytes0 = readVarIntBytes(encodedBytes, start + fieldNumber.length);
            int embeddedLength0 = (int)varIntBytesToObject(embeddedBytes0, Integer.class, null);
            byte[] stringBytes = Arrays.copyOfRange(encodedBytes, start + fieldNumber.length + embeddedBytes0.length, start + fieldNumber.length + embeddedBytes0.length + embeddedLength0);
            String stringValue = new String(stringBytes);
            return new ValueInfo(stringValue, fieldNumber.length + embeddedBytes0.length + embeddedLength0);
        }
        // write_type=5
        else if (Float.class == fieldType) {
            byte[] fieldNumber = readKeyOrValueFieldNumber(encodedBytes, start);
            ValueInfo valueInfo = read32BitValue(encodedBytes, start + fieldNumber.length, fieldType);
            return new ValueInfo(valueInfo.value, fieldNumber.length + valueInfo.valueLength);
        }
        else {
            byte[] fieldNumber = readKeyOrValueFieldNumber(encodedBytes, start);
            byte[] length = readMapKeyOrValueLength(encodedBytes, start + fieldNumber.length);
            int embeddedLength0 = (int)varIntBytesToObject(length, Integer.class, null);
            ValueInfo objectValueInfo = readObjectValue(encodedBytes, start + fieldNumber.length + length.length, start + fieldNumber.length + length.length + embeddedLength0, fieldType);
            return new ValueInfo(objectValueInfo.value, fieldNumber.length + length.length + embeddedLength0);
        }
    }

    public Object readVarIntValue(byte[] encodedBytes, int start, Class<?> valueType, FieldNumberInfo fieldNumber) {
        byte[] keyBytes = readVarIntBytes(encodedBytes, start);
        return varIntBytesToObject(keyBytes, valueType, fieldNumber);
    }

    private byte[] readVarIntBytes(byte[] encodedBytes, int start) {
        int offset = start;
        byte[] varIntBytes = new byte[0];
        while (offset < encodedBytes.length && (encodedBytes[offset] & 0x80) != 0) {
            varIntBytes = Bytes.appendByte(varIntBytes, encodedBytes[offset]);
            offset++;
        }
        if (offset < encodedBytes.length) {
            varIntBytes = Bytes.appendByte(varIntBytes, encodedBytes[offset++]);
        }
        return varIntBytes;
    }

    private Object varIntBytesToObject(byte[] varIntBytes, Class<?> valueType, FieldNumberInfo fieldNumber) {
        if (valueType == Integer.class || valueType == int.class) {
            Bits intBits = new Bits();
            for (int i = varIntBytes.length-1; i >= 0; i--) {
                intBits.append(Bits.ofByte(varIntBytes[i]).subBits(1, 8));
            }
            int val = intBits.toInt();
            // sint32（use zigzag encoding）
            if (fieldNumber != null && fieldNumber.type() == ProtoType.SINT32) {
                val = Bits.Decoder.decodeZigzagValue(val);
            }
            return val;
        } else if (valueType == Long.class || valueType == long.class) {
            Bits intBits = new Bits();
            for (int i = varIntBytes.length-1; i >= 0; i--) {
                intBits.append(Bits.ofByte(varIntBytes[i]).subBits(1, 8));
            }
            long val = intBits.toLong();
            if (fieldNumber != null && fieldNumber.type() == ProtoType.SINT64) {
                val = Bits.Decoder.decodeZigzagValue(val);
            }
            return val;
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return true;
        } else if (valueType.isEnum()) {
            Object[] enums = valueType.getEnumConstants();
            int enumIndex = (int)varIntBytesToObject(varIntBytes, Integer.class, null);
            return enums[enumIndex];
        } else {
            throw new IllegalArgumentException(valueType.getName() + " is not matched with varInt type");
        }
    }

    private ValueInfo read64BitValue(byte[] encodedBytes, int start, Class<?> fieldType) {
        double fieldValue = read64BitValue(Arrays.copyOfRange(encodedBytes, start, start + 8));
        return new ValueInfo(fieldValue, 8);
    }

    private ValueInfo readObjectValue(byte[] encodedBytes, int offset, int end, Class<?> fieldType) {
        Object value = newInstanceByNoArgsConstructorWithDefaultValue(fieldType);
        return readObjectValueThenSet(encodedBytes, offset, end, fieldType, value);
    }

    private ValueInfo read32BitValue(byte[] encodedBytes, int start, Class<?> fieldType) {
        float fieldValue = read32BitValue(Arrays.copyOfRange(encodedBytes, start, start + 4));
        return new ValueInfo(fieldValue, 4);
    }

    public double read64BitValue(byte[] encodedBytes) {
        for (int i = 0; i <= (encodedBytes.length >> 2); i++) {
            byte tmp = encodedBytes[i];
            encodedBytes[i] = encodedBytes[encodedBytes.length-i-1];
            encodedBytes[encodedBytes.length-i-1] = tmp;
        }
        return Bits.Decoder.decodeDoubleValue(Bits.ofByte(encodedBytes));
    }

    public float read32BitValue(byte[] encodedBytes) {
        for (int i = 0; i <= (encodedBytes.length >> 2); i++) {
            byte tmp = encodedBytes[i];
            encodedBytes[i] = encodedBytes[encodedBytes.length-i-1];
            encodedBytes[encodedBytes.length-i-1] = tmp;
        }
        return Bits.Decoder.decodeFloatValue(Bits.ofByte(encodedBytes));
    }
}
