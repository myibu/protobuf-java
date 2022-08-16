# Algorithm-Java
GRPC protocol implements in java

## Implements
See [protocol-buffers encoding](https://developers.google.cn/protocol-buffers/docs/encoding)

## Installation
```bash
<dependency>
  <groupId>com.github.myibu</groupId>
  <artifactId>protobuf-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Examples
```java
class OneClass {
    @FieldNumber(value = 1)
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}

ProtoMapper protoMapper = new ProtoMapper();
byte[] encodedBytes = protoMapper.writeObjectAsBytes(OneClass);
OneClass onClassObject = protoMapper.readObject(encodedBytes, OneClass.class);
```
## Change Notes
### 1.0.0
- support `list、array、map、String、int、long、float、double、boolean、byte[]` type
