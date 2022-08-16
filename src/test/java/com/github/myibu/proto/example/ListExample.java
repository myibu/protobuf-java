package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ListSimple
 *
 * @author hdh
 * Created on 2022/8/13
 */
public class ListExample {
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

    @FieldNumber(value = 1)
    private List<Integer> fieldList;

    @FieldNumber(value = 2)
    private List<String> fieldStringList;

    @FieldNumber(value = 3)
    private ArrayList<InnerId> fieldArrayList;

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

    public ArrayList<InnerId> getFieldArrayList() {
        return fieldArrayList;
    }

    public void setFieldArrayList(ArrayList<InnerId> fieldArrayList) {
        this.fieldArrayList = fieldArrayList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListExample that = (ListExample) o;
        return fieldList.equals(that.fieldList) && fieldStringList.equals(that.fieldStringList) && fieldArrayList.equals(that.fieldArrayList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldList, fieldStringList, fieldArrayList);
    }
}
