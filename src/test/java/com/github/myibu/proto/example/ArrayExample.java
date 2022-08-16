package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

import java.util.Arrays;

/**
 * ArrayExample
 *
 * @author hdh
 * Created on 2022/8/16
 */
public class ArrayExample {
    @FieldNumber(value = 1)
    private int[] fieldArray;

    public int[] getFieldArray() {
        return fieldArray;
    }

    public void setFieldArray(int[] fieldArray) {
        this.fieldArray = fieldArray;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayExample that = (ArrayExample) o;
        return Arrays.equals(fieldArray, that.fieldArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fieldArray);
    }
}
