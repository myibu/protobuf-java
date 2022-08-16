package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FullExample
 *
 * @author hdh
 * Created on 2022/8/11
 */
public class FullExample {
    public static class InnerId {
        public InnerId() {
        }

        public InnerId(int id) {
            this.id = id;
        }

        @FieldNumber(1)
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InnerId innerId = (InnerId) o;
            return id == innerId.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    // varInt
    @FieldNumber(1)
    private int fieldInt;

    @FieldNumber(2)
    private Boolean fieldBoolean;

    @FieldNumber(3)
    private Color color;

    // 64Bit
    @FieldNumber(4)
    private double fieldDouble;

    // LengthDelimited
    @FieldNumber(5)
    private String fieldString;

//    @FieldNumber(6)
//    private byte[] fieldBytes;

    @FieldNumber(7)
    private InnerId innerId;

    @FieldNumber(8)
    private List<String> repeated;

    // 32Bit
    @FieldNumber(9)
    private float fieldFloat;

    @FieldNumber(value = 10)
    private List<Integer> fieldList;

    @FieldNumber(value = 11)
    private List<String> fieldStringList;

    @FieldNumber(value = 12)
    private Map<String, InnerId> fieldMap;

    public int getFieldInt() {
        return fieldInt;
    }

    public void setFieldInt(int fieldInt) {
        this.fieldInt = fieldInt;
    }

    public Boolean getFieldBoolean() {
        return fieldBoolean;
    }

    public void setFieldBoolean(Boolean fieldBoolean) {
        this.fieldBoolean = fieldBoolean;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getFieldDouble() {
        return fieldDouble;
    }

    public void setFieldDouble(double fieldDouble) {
        this.fieldDouble = fieldDouble;
    }

    public String getFieldString() {
        return fieldString;
    }

    public void setFieldString(String fieldString) {
        this.fieldString = fieldString;
    }

//    public byte[] getFieldBytes() {
//        return fieldBytes;
//    }
//
//    public void setFieldBytes(byte[] fieldBytes) {
//        this.fieldBytes = fieldBytes;
//    }

    public InnerId getInnerId() {
        return innerId;
    }

    public void setInnerId(InnerId innerId) {
        this.innerId = innerId;
    }

    public List<String> getRepeated() {
        return repeated;
    }

    public void setRepeated(List<String> repeated) {
        this.repeated = repeated;
    }

    public float getFieldFloat() {
        return fieldFloat;
    }

    public void setFieldFloat(float fieldFloat) {
        this.fieldFloat = fieldFloat;
    }

    public List<Integer> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<Integer> fieldList) {
        this.fieldList = fieldList;
    }

    public List<String> getFieldStringList() {
        return fieldStringList;
    }

    public void setFieldStringList(List<String> fieldStringList) {
        this.fieldStringList = fieldStringList;
    }

    public Map<String, InnerId> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, InnerId> fieldMap) {
        this.fieldMap = fieldMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullExample that = (FullExample) o;
        return fieldInt == that.fieldInt && Double.compare(that.fieldDouble, fieldDouble) == 0 && Float.compare(that.fieldFloat, fieldFloat) == 0 && fieldBoolean.equals(that.fieldBoolean) && color == that.color && fieldString.equals(that.fieldString) && innerId.equals(that.innerId) && repeated.equals(that.repeated) && fieldList.equals(that.fieldList) && fieldStringList.equals(that.fieldStringList) && fieldMap.equals(that.fieldMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldInt, fieldBoolean, color, fieldDouble, fieldString, innerId, repeated, fieldFloat, fieldList, fieldStringList, fieldMap);
    }
}
