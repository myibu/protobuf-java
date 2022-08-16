# grpc
## install
- [javatutorial](https://developers.google.cn/protocol-buffers/docs/javatutorial)
- download[compiler](https://github.com/protocolbuffers/protobuf/releases/tag/v21.4)protoc-21.4-win64.zip
- use dependency`implementation 'com.google.protobuf:protobuf-java:3.21.4'`

## usage
1. define proto file first
2. generate java class by compiler with command `protoc --java_out=. ./simple.proto`
3. generate binary file for message by api