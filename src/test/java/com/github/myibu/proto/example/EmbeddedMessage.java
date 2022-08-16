package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

import java.util.Objects;

public class EmbeddedMessage {
    public EmbeddedMessage() {
    }

    public EmbeddedMessage(int id) {
        this.id = id;
    }

    @FieldNumber(value = 1)
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
        EmbeddedMessage that = (EmbeddedMessage) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}