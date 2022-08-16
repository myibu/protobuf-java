package com.github.myibu.proto.example;

import com.github.myibu.proto.annotation.FieldNumber;

/**
 * EmbeddedMessageExample
 *
 * @author hdh
 * Created on 2022/8/13
 */
public class EmbeddedMessageExample {
    public static class EmbeddedMessage {
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
    }

    @FieldNumber(value = 1)
    EmbeddedMessage message1;

    @FieldNumber(value = 2)
    EmbeddedMessage message2;

    public EmbeddedMessage getMessage1() {
        return message1;
    }

    public void setMessage1(EmbeddedMessage message1) {
        this.message1 = message1;
    }

    public EmbeddedMessage getMessage2() {
        return message2;
    }

    public void setMessage2(EmbeddedMessage message2) {
        this.message2 = message2;
    }
}
