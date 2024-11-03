package com.github.beothorn;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class Agent {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar javaAgentDisassembler.jar <class_file_path>");
            System.out.println("Usage: java -javaagent:javaAgentDisassembler.jar=MyClass -jar app.jar");
            return;
        }

        String filePath = args[0];
        Path path = Path.of(filePath);

        if(!Files.exists(path)){
            System.err.println("File not found: " + filePath);
            System.exit(1);
        }

        try {
            byte[] fileData = Files.readAllBytes(path);
            ByteCodeReader byteCodeReader = new ByteCodeReader(fileData);
            System.out.println(byteCodeReader);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public static void premain(
        String agentArgs,
        Instrumentation instrumentation
    ){
        try {
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(
                        ClassLoader loader,
                        String className,
                        Class<?> classBeingRedefined,
                        ProtectionDomain protectionDomain,
                        byte[] classfileBuffer
                ) {
                    if (className.contains(agentArgs)) {
                        try {
                            ByteCodeReader byteCodeReader = new ByteCodeReader(classfileBuffer);
                            System.out.println(byteCodeReader);
                        } catch (Exception e) {
                            System.out.println("Oops");
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            });
            System.out.println("Instrumentation started " + instrumentation);
        } catch (Exception e) {
            System.out.println("Something failed "+ e);
        }
    }
}

