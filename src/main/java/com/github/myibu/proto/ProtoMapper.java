package com.github.myibu.proto;

/**
 * ProtoMapper
 *
 * @author hdh
 * Created on 2022/8/5
 */
public class ProtoMapper implements ProtoDef {
    private final ProtoWriter writer = new ProtoWriter();
    private final ProtoReader reader = new ProtoReader();

    public ProtoWriter getWriter() {
        return writer;
    }

    public ProtoReader getReader() {
        return reader;
    }

    @Override
    public byte[] writeObjectAsBytes(Object value) {
        return writer.writeObjectAsBytes(value);
    }

    @Override
    public <T> T readObject(byte[] encodedBytes, Class<T> valueType) {
        return reader.readObject(encodedBytes, valueType);
    }
}
