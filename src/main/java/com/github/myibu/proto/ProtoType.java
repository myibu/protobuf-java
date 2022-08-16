package com.github.myibu.proto;

/**
 * ProtoDataType
 *
 * @author hdh
 * Created on 2022/8/12
 */
public enum ProtoType {
    /**
     * Java Type: double
     */
    DOUBLE,
    /**
     * Java Type: float
     */
    FLOAT,
    /**
     * Java Type: int
     */
    INT32,
    /**
     * Java Type: long
     */
    INT64,
    /**
     * Java Type: int[2]
     */
    UINT32,
    /**
     * Java Type: long[2]
     */
    UINT64,
    /**
     * Java Type: int
     */
    SINT32,
    /**
     * Java Type: long
     */
    SINT64,
    /**
     * Java Type: int[2]
     */
    FIXED32,
    /**
     * Java Type: long[2]
     */
    FIXED64,
    /**
     * Java Type: long[2]
     */
    SFIXED32,
    /**
     * Java Type: int[2]
     */
    SFIXED64,
    /**
     * Java Type: boolean
     */
    BOOL,
    /**
     * Java Type: String
     */
    STRING,
    /**
     * Java Type: ByteString
     */
    BYTES,
    /**
     * Java Type: none
     */
    UNDEFINED;
}
