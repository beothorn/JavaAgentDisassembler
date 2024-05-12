package org.example;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class JavaAgent {

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit(num & 0xF, 16);
        return new String(hexDigits);
    }


    public static void premain(
        String agentArgs,
        Instrumentation instrumentation
    ){
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer
            ) throws IllegalClassFormatException {
                if("com/example/Main".equals(className)){
                    System.out.println("Class byte");
                    System.out.println(Arrays.toString(classfileBuffer));
                    System.out.println("-----");
                    System.out.println("Class byte array as string");
                    System.out.println(new String(classfileBuffer, Charset.defaultCharset()));
                    System.out.println("----------------");
                    StringBuilder sb = new StringBuilder();

                    System.out.println("<<<<<<<<<<<");
                    System.out.println(
                            byteToHex(classfileBuffer[0])+" "+
                            byteToHex(classfileBuffer[1])+" "+
                            byteToHex(classfileBuffer[2])+" "+
                            byteToHex(classfileBuffer[3])+" "
                    );
                    // next four bytes are class version
                    try {
                        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(classfileBuffer));
                        int version = dataInputStream.readInt();// 0xCAFEBABE
                        System.out.println("V: "+version);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    int version =
                            ((classfileBuffer[4] & 0xFF) << 24) |
                            ((classfileBuffer[5] & 0xFF) << 16) |
                            ((classfileBuffer[6] & 0xFF) << 8) |
                            (classfileBuffer[7] & 0xFF);
                    System.out.println("Version: "+version);
                    int constPoolCount =
                            ((classfileBuffer[8] & 0xFF) << 8) |
                            (classfileBuffer[9] & 0xFF);
                    System.out.println("Constant pool size: "+constPoolCount);
                    // Valid constant pool indices start from 1
                    System.out.println(">>>>>>>>>>>");

                    byte[] ret = new byte[classfileBuffer.length];
                    int changed = 0;
                    for (int i = 0; i < classfileBuffer.length; i++) {
                        byte b = classfileBuffer[i];
                        ret[i] = b;
                        if(b == 11) changed++; // second 11 entry is the opcode to push 11
                        if(b == 11 && changed>1){
                            ret[i] = 12;
                        }
                        sb.append(byteToHex(b)).append(" ");
                    }
                    System.out.println("Class byte array in hexa");
                    System.out.println(sb);

                    return ret;
                }
                return null;
            }
        });
        System.out.println("Hello World "+instrumentation);
    }
}

