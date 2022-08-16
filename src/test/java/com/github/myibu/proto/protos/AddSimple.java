package com.github.myibu.proto.protos;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AddSimple {
  // Main function:  Reads the entire address book from a file,
  //   adds one person based on user input, then writes it back out to the same
  //   file.
  public static void main(String[] args) throws Exception {
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of(true, false)).build();
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of(17.625f, 17.625f)).build();
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of(150, 1)).build();
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of("ing", "test")).build();
          SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of(SimpleProto.InnerId.newBuilder().setId(150).build(),
            SimpleProto.InnerId.newBuilder().setId(150).build())).build();
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().addAllId(List.of(SimpleProto.InnerId.newBuilder().setId(150).setId2(150).build(),
//            SimpleProto.InnerId.newBuilder().setId(1).setId2(1).build())).build();
//    Map<String, SimpleProto.InnerId> map = new HashMap<>();
//    map.put("test", SimpleProto.InnerId.newBuilder().setId(150).build());
//    map.put("ing", SimpleProto.InnerId.newBuilder().setId(1).build());
//    map.put("1", SimpleProto.InnerId.newBuilder().setId(150).build());
//    for(Map.Entry<String, SimpleProto.InnerId> entry: map.entrySet()) {
//      System.out.println(entry.getKey());
//    }
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().putAllFieldMap(map).build();
//
//      Map<Integer, String> map = new HashMap<>();
//      map.put(1, "ing");
//      map.put(150, "test");
//      for(Map.Entry<Integer, String> entry: map.entrySet()) {
//        System.out.println(entry.getKey());
//      }
//      SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().putAllFieldMap(map).build();


//    Map<Integer, Float> map = new HashMap<>();
//    map.put(1, 17.625f);
//    map.put(150, 0.625f);
//    for(Map.Entry<Integer, Float> entry: map.entrySet()) {
//      System.out.println(entry.getKey());
//    }
//    SimpleProto.Simple simple = SimpleProto.Simple.newBuilder().putAllFieldMap(map).build();

    // Write the new address book back to disk.
    FileOutputStream output = new FileOutputStream("SIMPLE_FILE.txt");
    simple.writeTo(output);
    output.close();
  }
}