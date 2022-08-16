package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

/**
 * 注释
 *
 * @author hdh
 * Created on 2022/8/10
 */
public class MapExample {
    @FieldNumber(value = 1)
    private Map<String, EmbeddedMessage> fieldMap;

    @FieldNumber(value = 2)
    private HashMap<String, EmbeddedMessage> fieldHashMap;

    @FieldNumber(value = 3)
    private Map<Integer, Float> fieldPrimitiveMap;

    public Map<String, EmbeddedMessage> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, EmbeddedMessage> fieldMap) {
        this.fieldMap = fieldMap;
    }

    public HashMap<String, EmbeddedMessage> getFieldHashMap() {
        return fieldHashMap;
    }

    public void setFieldHashMap(HashMap<String, EmbeddedMessage> fieldHashMap) {
        this.fieldHashMap = fieldHashMap;
    }

    public Map<Integer, Float> getFieldPrimitiveMap() {
        return fieldPrimitiveMap;
    }

    public void setFieldPrimitiveMap(Map<Integer, Float> fieldPrimitiveMap) {
        this.fieldPrimitiveMap = fieldPrimitiveMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapExample that = (MapExample) o;
        return fieldMap.equals(that.fieldMap) && fieldHashMap.equals(that.fieldHashMap) && fieldPrimitiveMap.equals(that.fieldPrimitiveMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldMap, fieldHashMap, fieldPrimitiveMap);
    }
}
