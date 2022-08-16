package com.github.myibu.proto;

import com.github.myibu.algorithm.data.Bit;
import com.github.myibu.algorithm.data.Bits;
import com.github.myibu.algorithm.data.Bytes;
import com.github.myibu.proto.annotation.FieldNumber;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.myibu.proto.JavaBean.*;

/**
 * ProtoWriter
 *
 * @author hdh
 * Created on 2022/8/12
 */
public class ProtoWriter implements ProtoDef {
    @Override
    public byte[] writeObjectAsBytes(Object value) {
        return toObjectValue(value);
    }


    // write_type=0
    public byte[] writeVarIntValue(FieldNumberInfo fieldNumber, Object value) {
        if (ProtoDef.DefaultValue.anyMatch(value)) {
            return new byte[0];
        }
        byte[] key = toVarIntValue((fieldNumber.value() << KEY_TAG) | WRITE_TYPE_VAR_INT);
        return writeVarIntValue(key, value, fieldNumber.type());
    }

    // write_type=0
    private byte[] writeVarIntValue(byte[] key, Object value, ProtoType type) {
        byte[] intValue = toVarIntValue(value, type);
        return Bytes.appendBytes(key, intValue);
    }

    // write_type=1, finished
    public byte[] write64BitValue(FieldNumberInfo fieldNumber, Object value) {
        if (ProtoDef.DefaultValue.anyMatch(value)) {
            return new byte[0];
        }
        byte[] key = toVarIntValue((fieldNumber.value() << KEY_TAG) | WRITE_TYPE_64_BIT);
        return write64BitValue(key, value, fieldNumber.type());
    }

    // write_type=1, finished
    private byte[] write64BitValue(byte[] key, Object value, ProtoType type) {
        if (value.getClass() == Double.class) {
            byte[] doubleValue = toDoubleValue((double) value);
            return Bytes.appendBytes(key, doubleValue);
        }
        return null;
    }

    // write_type=2
    public byte[] writeLengthDelimitedValue(FieldNumberInfo fieldNumber, Object value) {
        if (ProtoDef.DefaultValue.anyMatch(value)) {
            return new byte[0];
        }
        byte[] key = toVarIntValue((fieldNumber.value() << KEY_TAG) | WRITE_TYPE_LENGTH_DELIMITED_BIT);
        return writeLengthDelimitedValue (key, value, fieldNumber.type());
    }

    // write_type=2
    private byte[] writeLengthDelimitedValue(byte[] key, Object value, ProtoType type) {
        // strings
        if (value.getClass() == String.class) {
            byte[] stringValue = toStringValue((String)value);
            return Bytes.appendBytes(key, stringValue);
        }
        // packed repeated fields
        else if (Iterable.class.isAssignableFrom(value.getClass())) {
            // not process empty collection
            if (!((Iterable<?>) value).iterator().hasNext()) {
                return new byte[0];
            }
            Class<?> repeatedType = getListOrArrayRepeatedType(value);
            // repeated fields of scalar numeric types use packed encoding by default.
            boolean usePacked = usePackedForType(repeatedType);
            if (usePacked) {
                byte[] repeatedBytes = new byte[0];
                for (Object ob: (Iterable)value) {
                    byte[] repeatedByte = writeRepeatedBytes(ob, repeatedType);
                    repeatedBytes = Bytes.appendBytes(repeatedBytes, repeatedByte);
                }
                byte[] repeatedLength = toVarIntValue(repeatedBytes.length);
                return (repeatedBytes.length > 0) ? Bytes.appendBytes(key, Bytes.appendBytes(repeatedLength, repeatedBytes)) : new byte[0];
            } else {
                byte[] repeatedBytes = new byte[0];
                for (Object ob: (Iterable)value) {
                    byte[] repeatedByte = writeRepeatedBytes(ob, repeatedType);
                    repeatedBytes = Bytes.appendBytes(repeatedBytes, Bytes.appendBytes(key, repeatedByte));
                }
                return repeatedBytes;
            }
        }
        // array
        else if (value.getClass().isArray()) {
            // not process empty collection
            Class<?> repeatedType = getListOrArrayRepeatedType(value);
            // write byes like string
            if (byte.class == repeatedType || Byte.class == repeatedType) {
                byte[] bytesValue = toBytesValue((byte[])value);
                return Bytes.appendBytes(key, bytesValue);
            }
            // repeated fields of scalar numeric types use packed encoding by default.
            boolean usePacked = usePackedForType(repeatedType);
            if (usePacked) {
                // convert primitive data type to packed Object
                Object[] array = getArrayPackagedValue(value, repeatedType);
                byte[] repeatedBytes = new byte[0];
                for (Object ob: array) {
                    byte[] repeatedByte = writeRepeatedBytes(ob, repeatedType);
                    repeatedBytes = Bytes.appendBytes(repeatedBytes, repeatedByte);
                }
                byte[] repeatedLength = toVarIntValue(repeatedBytes.length);
                return (repeatedBytes.length > 0) ? Bytes.appendBytes(key, Bytes.appendBytes(repeatedLength, repeatedBytes)) : new byte[0];
            } else {
                // convert primitive data type to packed Object
                Object[] array = getArrayPackagedValue(value, repeatedType);
                byte[] repeatedBytes = new byte[0];
                for (Object ob: array) {
                    byte[] repeatedByte = writeRepeatedBytes(ob, repeatedType);
                    repeatedBytes = Bytes.appendBytes(repeatedBytes, Bytes.appendBytes(key, repeatedByte));
                }
                return repeatedBytes;
            }
        }
        // map
        else if (Map.class.isAssignableFrom(value.getClass())) {
            byte[] mapBytes = new byte[0];
            Iterator<Map.Entry> iterator = ((Map)value).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry mapItem = iterator.next();
                byte[] keyBytes = writeMapKeyOrValue(new FieldNumberInfo(MAP_KEY_FIELD_NUMBER, ProtoType.UNDEFINED), mapItem.getKey());
                byte[] valueBytes =  writeMapKeyOrValue(new FieldNumberInfo(MAP_VALUE_FIELD_NUMBER, ProtoType.UNDEFINED), mapItem.getValue());
                byte[] mapLength = toVarIntValue(keyBytes.length + valueBytes.length);
                byte[] mapItemBytes = Bytes.appendBytes(mapLength, Bytes.appendBytes(keyBytes, valueBytes));
                mapBytes = Bytes.appendBytes(mapBytes, Bytes.appendBytes(key, mapItemBytes));
            }
            return mapBytes;
        }
        // embedded messages
        else {
            byte[] embeddedBytes = toObjectValue(value);
            byte[] embeddedLength = toVarIntValue(embeddedBytes.length);
            return (embeddedBytes.length > 0) ? Bytes.appendBytes(key, Bytes.appendBytes(embeddedLength, embeddedBytes)) : new byte[0];
        }
    }

    private byte[] writeRepeatedBytes(Object value, Class<?> valueType) {
        byte[] encodedBytes;
        // write_type=0
        if (Integer.class == valueType || int.class == valueType
                || Long.class == valueType || long.class == valueType
                || Boolean.class == valueType || boolean.class == valueType
                || valueType.isEnum()) {
            encodedBytes = toVarIntValue(value);
        }
        // write_type=1
        else if (Double.class == valueType || double.class == valueType) {
            encodedBytes = toDoubleValue((double)value);
        }
        // write_type=2
        else if (String.class == valueType) {
            encodedBytes = toStringValue((String)value);
        }
        // write_type=5
        else if (Float.class == valueType || float.class == valueType) {
            encodedBytes = toFloatValue((float)value);
        }
        else {
            encodedBytes = toObjectValue(value);
            byte[] objectLength = toVarIntValue(encodedBytes.length);
            encodedBytes = Bytes.appendBytes(objectLength, encodedBytes);
        }
        return encodedBytes;
    }

    public byte[] writeKeyOrValueLength(int length) {
        return toVarIntValue(length, null);
    }

    // write_type=2
    public byte[] writeMapKeyOrValue(FieldNumberInfo fieldNumber, Object value) {
        byte[] encodedBytes;
        Class<?> valueType = value.getClass();
        // write_type=0
        if (Integer.class == valueType || Long.class == valueType || Boolean.class == valueType || valueType.isEnum()) {
            encodedBytes = writeVarIntValue(fieldNumber, value);
        }
        // write_type=1
        else if (Double.class == valueType) {
            encodedBytes = write64BitValue(fieldNumber, value);
        }
        // write_type=2
        else if (String.class == valueType) {
            encodedBytes = writeLengthDelimitedValue(fieldNumber, value);
        }
        // write_type=5
        else if (Float.class == valueType) {
            encodedBytes = write32BitValue(fieldNumber, value);
        }
        else {
            encodedBytes = writeLengthDelimitedValue(fieldNumber, value);
        }
        return encodedBytes;
    }


    // write_type=3
    public byte[] writeStartGroupValue(FieldNumberInfo fieldNumber, Object value) {
        throw new UnsupportedOperationException("not support write_type=3");
    }

    // write_type=4
    public byte[] writeEndGroupValue(FieldNumberInfo fieldNumber, Object value) {
        throw new UnsupportedOperationException("not support write_type=4");
    }

    // write_type=5, finished
    public byte[] write32BitValue(FieldNumberInfo fieldNumber, Object value) {
        if (ProtoDef.DefaultValue.anyMatch(value)) {
            return new byte[0];
        }
        byte[] key = toVarIntValue((fieldNumber.value() << KEY_TAG) | WRITE_TYPE_32_BIT);
        return write32BitValue(key, value, fieldNumber.type());
    }

    // write_type=5, finished
    private byte[] write32BitValue(byte[] key, Object value, ProtoType type) {
        if (value.getClass() == Float.class) {
            byte[] floatValue = toFloatValue((float) value);
            return Bytes.appendBytes(key, floatValue);
        }
        return null;
    }

    private byte[] toObjectValue(Object value) {
        Class<?> targetClass = value.getClass();
        List<Field> validFields = Arrays.stream(targetClass.getDeclaredFields())
                .filter(field -> null != field.getAnnotation(FieldNumber.class))
                .collect(Collectors.toList());
        byte[] encodedBytes = new byte[0];
        for (Field validField: validFields) {
            FieldNumberInfo fieldNumber = new FieldNumberInfo(validField.getAnnotation(FieldNumber.class));
            Class<?> fieldType = validField.getType();
            Object fieldValue = getFieldValue(validField, value);
            // not write null value
            if (fieldValue == null) continue;
            // write_type=0
            if (Integer.class == fieldType || int.class == fieldType ||
                    Long.class == fieldType  || long.class == fieldType ||
                    Boolean.class == fieldType || boolean.class == fieldType ||
                    fieldType.isEnum()) {
                byte[] bytes = writeVarIntValue(fieldNumber, fieldValue);
                encodedBytes = Bytes.appendBytes(encodedBytes, bytes);
            }
            // write_type=1
            else if (Double.class == fieldType || double.class == fieldType) {
                byte[] bytes = write64BitValue(fieldNumber, fieldValue);
                encodedBytes = Bytes.appendBytes(encodedBytes, bytes);
            }
            // write_type=2
            else if (String.class == fieldType
                    || Iterable.class.isAssignableFrom(fieldType)
                    || fieldType.isArray()
                    || Map.class.isAssignableFrom(fieldType)) {
                byte[] bytes = writeLengthDelimitedValue(fieldNumber, fieldValue);
                encodedBytes = Bytes.appendBytes(encodedBytes, bytes);
            }
            // write_type=5
            else if (Float.class == fieldType || float.class == fieldType) {
                byte[] bytes = write32BitValue(fieldNumber, fieldValue);
                encodedBytes = Bytes.appendBytes(encodedBytes, bytes);
            } else {
                byte[] bytes = writeLengthDelimitedValue(fieldNumber, fieldValue);
                encodedBytes = Bytes.appendBytes(encodedBytes, bytes);
            }
        }
        return encodedBytes;
    }

    private byte[] toVarIntValue(Object value, ProtoType type) {
        byte[] groupBytes = null;
        if (value.getClass() == Integer.class) {
            int val = (int)value;
            // sint32（use zigzag encoding）
            if (type == ProtoType.SINT32) {
                val = Bits.Encoder.encodeZigzagValue(val);
            }
            Bits binaryArr = Bits.Encoder.encodeIntValue(val);
            int groupNum = (int)Math.ceil(binaryArr.length() * 1.0 / VAR_INT_GROUP_LENGTH), i = 0, end = binaryArr.length();
            groupBytes = new byte[groupNum];
            while (i < groupNum) {
                Bits group = Bits.ofZero(8);
                if (i != groupNum-1) {
                    group.set(0, Bit.ONE);
                }
                int srcStart = Math.max(end - VAR_INT_GROUP_LENGTH, 0);
                int destStart = end < VAR_INT_GROUP_LENGTH ? VAR_INT_GROUP_LENGTH - end + 1 : 1;
                int length = Math.min(end, VAR_INT_GROUP_LENGTH);
                Bits.copy(binaryArr, srcStart, group, destStart, length);
                groupBytes[i] = group.toByte();
                end = end-VAR_INT_GROUP_LENGTH;
                i++;
            }
        } else if (value.getClass() == Long.class) {
            long val = (long)value;
            // sint64（use zigzag encoding）
            if (type == ProtoType.SINT64) {
                val = Bits.Encoder.encodeZigzagValue(val);
            }
            Bits binaryArr = Bits.ofString(Long.toBinaryString(val));
            int groupNum = (int)Math.ceil(binaryArr.length() * 1.0 / VAR_INT_GROUP_LENGTH), i = 0, end = binaryArr.length();
            groupBytes = new byte[groupNum];
            while (i < groupNum) {
                Bits group = Bits.ofZero(8);
                if (i != groupNum-1) {
                    group.set(0, Bit.ONE);
                }
                int srcStart = Math.max(end - VAR_INT_GROUP_LENGTH, 0);
                int destStart = end < VAR_INT_GROUP_LENGTH ? VAR_INT_GROUP_LENGTH - end + 1 : 1;
                int length = Math.min(end, VAR_INT_GROUP_LENGTH);
                Bits.copy(binaryArr, srcStart, group, destStart, length);
                groupBytes[i] = group.toByte();
                end = end-VAR_INT_GROUP_LENGTH;
                i++;
            }
        } else if (value.getClass() == Boolean.class) {
            boolean val = (boolean) value;
            groupBytes = val ? Bits.ofString("00000001").toByteArray() : Bits.ofString("00000000").toByteArray();
        } else if (value.getClass().isEnum()) {
            int enumOrder = 0;
            Object[] enums = value.getClass().getEnumConstants();
            for (int i = 0; i < enums.length; i++) {
                if (value == enums[i]) {
                    enumOrder = i;
                    break;
                }
            }
            return toVarIntValue(enumOrder);
        }
        return groupBytes;
    }

    private byte[] toVarIntValue(Object value) {
        return toVarIntValue(value, ProtoType.UNDEFINED);
    }

    private byte[] toStringValue(String value) {
        byte[] stringValue = Bits.Encoder.encodeStringValue(value).toByteArray();
        byte[] stringLength = toVarIntValue(stringValue.length);
        return Bytes.appendBytes(stringLength, stringValue);
    }

    private byte[] toBytesValue(byte[] value) {
        byte[] bytesLength = toVarIntValue(value.length);
        return Bytes.appendBytes(bytesLength, value);
    }

    private byte[] toDoubleValue(double value) {
        byte[] doubleValue = Bits.Encoder.encodeDoubleValue(value).toByteArray();
        // little-endian
        for (int i = 0; i <= (doubleValue.length >> 2); i++) {
            byte tmp = doubleValue[i];
            doubleValue[i] = doubleValue[doubleValue.length-i-1];
            doubleValue[doubleValue.length-i-1] = tmp;
        }
        return doubleValue;
    }

    private byte[] toFloatValue(float value) {
        byte[] floatValue = Bits.Encoder.encodeFloatValue(value).toByteArray();
        // little-endian
        for (int i = 0; i <= (floatValue.length >> 2); i++) {
            byte tmp = floatValue[i];
            floatValue[i] = floatValue[floatValue.length-i-1];
            floatValue[floatValue.length-i-1] = tmp;
        }
        return floatValue;
    }
}
