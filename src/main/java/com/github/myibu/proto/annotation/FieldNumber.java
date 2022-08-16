package com.github.myibu.proto.annotation;

import com.github.myibu.proto.ProtoType;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldNumber {
    int value();

    ProtoType type() default ProtoType.UNDEFINED;

    boolean useDefaultValue() default false;
}
