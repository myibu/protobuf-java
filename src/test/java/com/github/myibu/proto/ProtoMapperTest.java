package com.github.myibu.proto;

import com.github.myibu.algorithm.data.Bits;
import com.github.myibu.algorithm.data.Bytes;
import com.github.myibu.proto.example.*;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * 注释
 *
 * @author hdh
 * Created on 2022/8/9
 */
public class ProtoMapperTest {
    @Test
    public void testRepeatedType() {
        Assert.assertEquals(int.class, JavaBean.getListOrArrayRepeatedType(new int[]{1, 2}));
        Assert.assertEquals(Double.class, JavaBean.getListOrArrayRepeatedType(new Double[]{1.0, 2.0}));
        Assert.assertEquals(Integer.class, JavaBean.getListOrArrayRepeatedType(List.of(150, 1)));
    }

    static class HexAssert extends Assert {
        public static void assertEquals(String expected, String actual) {
            expected = expected.replaceAll("[ \\[\\]\\{\\}]", "");
            assertTrue(expected.equalsIgnoreCase(actual));
        }
    }

    @Test
    public void testDefaultValue() {
        //For strings, the default value is the empty string.
        Assert.assertTrue(ProtoDef.DefaultValue.anyMatch(""));
        Assert.assertFalse(ProtoDef.DefaultValue.anyMatch("testing"));

        //For bytes, the default value is empty bytes.
        Assert.assertTrue(ProtoDef.DefaultValue.anyMatch(new byte[0]));
        Assert.assertFalse(ProtoDef.DefaultValue.anyMatch(new byte[]{1}));

        //For bools, the default value is false.
        Assert.assertTrue(ProtoDef.DefaultValue.anyMatch(false));
        Assert.assertFalse(ProtoDef.DefaultValue.anyMatch(true));

        //For numeric types, the default value is zero.
        Assert.assertTrue(ProtoDef.DefaultValue.anyMatch(0));
        Assert.assertFalse(ProtoDef.DefaultValue.anyMatch(1));

        //For numeric types, the default value is zero.
        Assert.assertTrue(ProtoDef.DefaultValue.anyMatch(Color.RED));
        Assert.assertFalse(ProtoDef.DefaultValue.anyMatch(Color.BLACK));
    }

    @Test
    public void testWriteVarInt() {
        ProtoMapper protoMapper = new ProtoMapper();
        // int32
        HexAssert.assertEquals(
                "[08] [96 01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1), 150)).toByteArray())
        );

        // sint32
        HexAssert.assertEquals(
                "[08] [02]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1, ProtoType.SINT32), 1)).toByteArray())
        );

        // bool
        HexAssert.assertEquals(
                "[08] [01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1), true)).toByteArray())
        );
        HexAssert.assertEquals(
                "[]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1), false)).toByteArray())
        );

        // enum
        HexAssert.assertEquals(
                "[08] [01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1), Color.BLACK)).toByteArray())
        );
        HexAssert.assertEquals(
                "[]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeVarIntValue(new ProtoDef.FieldNumberInfo(1), Color.RED)).toByteArray())
        );
    }

    @Test
    public void testWriteLengthDelimited() {
        ProtoMapper protoMapper = new ProtoMapper();
        // strings
        HexAssert.assertEquals(
                "[12] [07] [74 65 73 74 69 6e 67]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(2), "testing")).toByteArray())
        );

        // bytes
        HexAssert.assertEquals(
                "[0A] [02] [3E] [8D]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), new byte[]{62, -115})).toByteArray())
        );

        // embedded messages
        EmbeddedMessageExample message = new EmbeddedMessageExample();
        message.setMessage1(new EmbeddedMessageExample.EmbeddedMessage(150));
        message.setMessage2(new EmbeddedMessageExample.EmbeddedMessage(1));
        HexAssert.assertEquals(
                "{[0A] [03] [08 96 01]} {[12] [02] [08 01]}",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeObjectAsBytes(message)).toByteArray())
        );

        // packed repeated fields
        // repeated: int32
        HexAssert.assertEquals(
                "[0A] [03] [96 01] [01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), List.of(150, 1))).toByteArray())
        );
        // repeated: float
        HexAssert.assertEquals(
                "[0A] [08] [00 00 8D 41] [00 00 8D 41]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), List.of(17.625f, 17.625f))).toByteArray())
        );
        // repeated: bool
        HexAssert.assertEquals(
                "[0A] [02] [01] [00]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), List.of(true, false))).toByteArray())
        );
        // repeated: enum
        HexAssert.assertEquals(
                "[0A] [02] [00] [01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), List.of(Color.RED, Color.BLACK))).toByteArray())
        );
        // repeated: strings
        HexAssert.assertEquals(
                "[0A] [03 69 6E 67] [0A] [04 74 65 73 74]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), List.of("ing", "test"))).toByteArray())
        );
        // repeated: InnerId
        HexAssert.assertEquals(
                "[1A] [03 08 96 01] [1A] [03 08 96 01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(3), List.of(new ListExample.InnerId(150), new ListExample.InnerId(150)))).toByteArray())
        );

        // map
        Map<Integer, Float> intFloatMap = new HashMap<>();
        intFloatMap.put(1, 17.625f);
        intFloatMap.put(150, 0.625f);
        HexAssert.assertEquals(
                "[0A] [07] [08 01] [15 00 00 8D 41] [0A] [08] [08 96 01] [15 00 00 20 3F]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), intFloatMap)).toByteArray())
        );

        Map<Integer, String> intStringMap = new HashMap<>();
        intStringMap.put(1, "ing");
        intStringMap.put(150, "test");
        HexAssert.assertEquals(
                "[0A] [07] [08 01] [12 03 69 6E 67] [0A] [09] [08 96 01 12 04 74 65 73 74]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), intStringMap)).toByteArray())
        );

        Map<String, FullExample.InnerId> stringObjectMap = new HashMap<>();
        stringObjectMap.put("test", new FullExample.InnerId(150));
        stringObjectMap.put("ing", new FullExample.InnerId(1));
        HexAssert.assertEquals(
                "[0A] [09] [0A 03 69 6E 67] [12 02 08 01] [0A] [0B] [0A 04 74 65 73 74] [12 03 08 96 01]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().writeLengthDelimitedValue(new ProtoDef.FieldNumberInfo(1), stringObjectMap)).toByteArray())
        );
    }

    @Test
    public void testWrite64Bit() {
        ProtoMapper protoMapper = new ProtoMapper();
        HexAssert.assertEquals(
                "[09] [00 00 00 00 00 A0 31 40]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().write64BitValue(new ProtoDef.FieldNumberInfo(1), 17.625)).toByteArray())
        );
    }

    @Test
    public void testWriteStartGroup() {
    }

    @Test
    public void testWriteEndGroup() {
    }

    @Test
    public void testWrite32Bit() {
        ProtoMapper protoMapper = new ProtoMapper();
        HexAssert.assertEquals(
                "[0D] [00 00 8D 41]",
                Bytes.byteArrayToHexString(Bits.ofByte(protoMapper.getWriter().write32BitValue(new ProtoDef.FieldNumberInfo(1), 17.625f)).toByteArray())
        );
    }

    @Test
    public void testReadVarInt() {
        ProtoMapper protoMapper = new ProtoMapper();
        // int32
        Assert.assertEquals(150, protoMapper.getReader().readVarIntValue(Bits.ofString("1001011000000001").toByteArray(), 0, Integer.class, null));

        // sint32
        Assert.assertEquals(1, protoMapper.getReader().readVarIntValue(Bits.ofString("00000010").toByteArray(), 0, Integer.class, new ProtoDef.FieldNumberInfo(1, ProtoType.SINT32)));

        // bool
        Assert.assertEquals(true, protoMapper.getReader().readVarIntValue(Bits.ofString("00000001").toByteArray(), 0, Boolean.class, null));

        // enum
        Assert.assertEquals(Color.WHITE, protoMapper.getReader().readVarIntValue(Bits.ofString("00000010").toByteArray(), 0, Color.class, null));
    }

    @Test
    public void testRead64Bit() {
        ProtoMapper protoMapper = new ProtoMapper();
        Assert.assertEquals(17.625, protoMapper.getReader().read64BitValue(Bits.ofString("0000000000000000000000000000000000000000101000000011000101000000").toByteArray()), 0);
    }

    @Test
    public void testRead32Bit() {
        ProtoMapper protoMapper = new ProtoMapper();
        Assert.assertEquals(17.625f, protoMapper.getReader().read32BitValue(Bits.ofString("00000000000000001000110101000001").toByteArray()), 0);
    }

    @Test
    public void testArrayReadWrite() {
        ProtoMapper protoMapper = new ProtoMapper();
        // write
        ArrayExample message = new ArrayExample();
        message.setFieldArray(new int[]{150, 150});
        byte[] encodedBytes = protoMapper.writeObjectAsBytes(message);

        // read
        ArrayExample newMessage = protoMapper.readObject(encodedBytes, ArrayExample.class);
        Assert.assertEquals(message, newMessage);
    }

    @Test
    public void testListReadWrite() {
        ProtoMapper protoMapper = new ProtoMapper();
        // write
        ListExample message = new ListExample();
        message.setFieldList(List.of(150, 150));
        message.setFieldStringList(List.of("ing", "test"));
        ArrayList list = new ArrayList();
        list.add(new ListExample.InnerId(150));
        list.add(new ListExample.InnerId(150));
        message.setFieldArrayList(list);
        byte[] encodedBytes = protoMapper.writeObjectAsBytes(message);

        // read
        ListExample newMessage = protoMapper.readObject(encodedBytes, ListExample.class);
        Assert.assertEquals(message, newMessage);
    }

    @Test
    public void testMapReadWrite() {
        ProtoMapper protoMapper = new ProtoMapper();
        // write
        MapExample message = new MapExample();
        Map<String, EmbeddedMessage> map = new Hashtable<>();
        map.put("test", new EmbeddedMessage(150));
        map.put("ing", new EmbeddedMessage(1));
        message.setFieldMap(map);

        HashMap<String, EmbeddedMessage> hashMap = new HashMap<>();
        hashMap.put("test", new EmbeddedMessage(150));
        hashMap.put("ing", new EmbeddedMessage(1));
        message.setFieldHashMap(hashMap);

        Map<Integer, Float> primitiveMap = new HashMap<>();
        primitiveMap.put(1, 17.625f);
        primitiveMap.put(150, 0.625f);
        message.setFieldPrimitiveMap(primitiveMap);
        byte[] encodedBytes = protoMapper.writeObjectAsBytes(message);

        // read
        MapExample newMessage = protoMapper.readObject(encodedBytes, MapExample.class);
        Assert.assertEquals(message, newMessage);
    }

    @Test
    public void testFullExample() {
        ProtoMapper protoMapper = new ProtoMapper();

        FullExample fullExample = new FullExample();
        fullExample.setFieldInt(150);
        fullExample.setFieldBoolean(true);
        fullExample.setColor(Color.BLACK);
        fullExample.setFieldDouble(17.625);
        fullExample.setFieldString("testing");
        // 0011111010001101
//        fullExample.setFieldBytes(new byte[]{62, -115});
        fullExample.setInnerId(new FullExample.InnerId(150));
        fullExample.setRepeated(List.of("1", "2"));
        fullExample.setFieldFloat(0.625f);
            Map<String, FullExample.InnerId> map = new HashMap<>();
            map.put("test", new FullExample.InnerId(150));
            map.put("ing", new FullExample.InnerId(1));
        fullExample.setFieldList(List.of(150, 150));
        fullExample.setFieldStringList(List.of("ing", "test"));
        fullExample.setFieldMap(map);
        byte[] encodedBytes = protoMapper.writeObjectAsBytes(fullExample);

        JSONObject j = new JSONObject(fullExample);
        System.out.println(j);
        FullExample newFullExample = protoMapper.readObject(encodedBytes, FullExample.class);
        Assert.assertEquals(fullExample, newFullExample);
    }
}
