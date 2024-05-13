package org.example;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class JavaAgent {

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
            ){
                if("com/example/Main".equals(className)){
                    try {
                        ByteCodeReader byteCodeReader = new ByteCodeReader(classfileBuffer);
                        System.out.println(byteCodeReader);
                    } catch (Exception e){
                        System.out.println("Oops");
                        e.printStackTrace();
                    }
                }
                return null;
            }
        });
        System.out.println("Instrumentation started "+instrumentation);
    }

    // See https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
    private enum ConstantType {
        CONSTANT_Class((byte) 7),
        CONSTANT_Fieldref((byte)9),
        CONSTANT_Methodref((byte)10),
        CONSTANT_InterfaceMethodref((byte)11),
        CONSTANT_String((byte)8),
        CONSTANT_Integer((byte)3),
        CONSTANT_Float((byte)4),
        CONSTANT_Long((byte)5),
        CONSTANT_Double((byte)6),
        CONSTANT_NameAndType((byte)12),
        CONSTANT_Utf8((byte)1),
        CONSTANT_MethodHandle((byte)15),
        CONSTANT_MethodType((byte)16),
        CONSTANT_InvokeDynamic((byte)18);

        private final byte tag;

        ConstantType(byte tag) {
            this.tag = tag;
        }

        public byte getTag() {
            return tag;
        }

        public static ConstantType fromTag(byte tag) {
            return Arrays.stream(ConstantType.values())
                    .filter(c -> c.getTag() == tag)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid constant tag: " + tag));
        }

    }

    private interface ConstantWithValue {
        default String print(Constant[] constants){
            return toString();
        }
    }

    private record Constant(ConstantType type, ConstantWithValue constantValue){}

    // TODO: Check each constant and print appropriate value

    private record ConstantClass(short nameIndex) implements ConstantWithValue {
        @Override
        public String print(final Constant[] constants) {
            return "Class" + constants[nameIndex].constantValue.print(constants);
        }
    }
    private record ConstantFieldRef(short classIndex, short nameAndTypeIndex) implements ConstantWithValue {
        @Override
        public String print(final Constant[] constants) {
            return "Class: "+constants[classIndex].constantValue.print(constants)+" Field: " + constants[nameAndTypeIndex].constantValue.print(constants);
        }
    }
    private record ConstantMethodRef(short classIndex, short nameAndTypeIndex) implements ConstantWithValue {
    }
    private record ConstantInterfaceMethodRef(short classIndex, short nameAndTypeIndex) implements ConstantWithValue {}
    private record ConstantString(short stringIndex) implements ConstantWithValue {}
    private record ConstantInteger(int bytes) implements ConstantWithValue {}
    private record ConstantFloat(int bytes) implements ConstantWithValue {}
    private record ConstantLong(int highBytes, int lowBytes) implements ConstantWithValue {}
    private record ConstantDouble(int highBytes, int lowBytes) implements ConstantWithValue {}
    private record ConstantNameAndType(short nameIndex, short descriptorIndex) implements ConstantWithValue {
        @Override
        public String print(final Constant[] constants) {
            return "Name: "+constants[nameIndex].constantValue.print(constants)+" Type: " + constants[descriptorIndex].constantValue.print(constants);
        }
    }
    private record ConstantUtf8(String value) implements ConstantWithValue {
        @Override
        public String print(final Constant[] constants) {
            return value;
        }
    }
    private record ConstantMethodHandle(byte referenceKind, short referenceIndex) implements ConstantWithValue {}
    private record ConstantMethodType(short descriptorIndex) implements ConstantWithValue {}
    private record ConstantInvokeDynamic(short bootstrapMethodAttrIndex, short nameAndTypeIndex) implements ConstantWithValue {
        @Override
        public String print(final Constant[] constants) {
            return "BootstrapMethod: "+constants[bootstrapMethodAttrIndex].constantValue.print(constants)+" Type: " + constants[nameAndTypeIndex].constantValue.print(constants);
        }
    }

    private static class ByteCodeReader {
        public final int magicNumber;
        public final short minorVersion;
        public final short majorVersion;

        // public final short accessFlags;
        // public final short thisClass;
        // public final short superClass;
        // public final short interfacesCount;
        // public final short[] interfaces;
        // public final field_info     fields[fields_count];
        // public final method_info    methods[methods_count];
        // public final attribute_info attributes[attributes_count];

        public final String version;
        public final Constant[] constantPool;

        ByteCodeReader(byte[] data){
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN);
            magicNumber = buffer.getInt();
            minorVersion = buffer.getShort();
            majorVersion = buffer.getShort();
            version = getPublicVersionNameForClassVersion(majorVersion);
            // The value of the constant_pool_count item is equal to the number of entries in the constant_pool table plus one.
            constantPool = new Constant[buffer.getShort() - 1];
            for (short i = 0; i < constantPool.length; i++) {
                byte tag = buffer.get();
                ConstantType tagEnum = ConstantType.fromTag(tag);
                ConstantWithValue value = readNext(tagEnum, buffer);
                constantPool[i] = new Constant(tagEnum, value);
            }
        }

        private ConstantWithValue readNext(
            final ConstantType tagEnum,
            final ByteBuffer buffer
        ) {
            return switch (tagEnum) {
                case CONSTANT_Class -> new ConstantClass(buffer.getShort());
                case CONSTANT_Fieldref -> new ConstantFieldRef(buffer.getShort(), buffer.getShort());
                case CONSTANT_Methodref -> new ConstantMethodRef(buffer.getShort(), buffer.getShort());
                case CONSTANT_InterfaceMethodref -> new ConstantInterfaceMethodRef(buffer.getShort(), buffer.getShort());
                case CONSTANT_String -> new ConstantString(buffer.getShort());
                case CONSTANT_Integer -> new ConstantInteger(buffer.getInt());
                case CONSTANT_Float -> new ConstantFloat(buffer.getInt());
                case CONSTANT_Long -> new ConstantLong(buffer.getInt(), buffer.getInt());
                case CONSTANT_Double -> new ConstantDouble(buffer.getInt(), buffer.getInt());
                case CONSTANT_NameAndType -> new ConstantNameAndType(buffer.getShort(), buffer.getShort());
                case CONSTANT_Utf8 -> {
                    short stringSize = buffer.getShort();
                    byte[] read = new byte[stringSize];
                    buffer.get(read, buffer.arrayOffset(), read.length);
                    yield new ConstantUtf8(new String(read, StandardCharsets.UTF_8));
                }
                case CONSTANT_MethodHandle -> new ConstantMethodHandle(buffer.get(), buffer.getShort());
                case CONSTANT_MethodType -> new ConstantMethodType(buffer.getShort());
                case CONSTANT_InvokeDynamic -> new ConstantInvokeDynamic(buffer.getShort(), buffer.getShort());
            };
        }

        private static String getPublicVersionNameForClassVersion(int version) {
            return switch (version) {
                case 66 -> "Java SE 22";
                case 65 -> "Java SE 21";
                case 64 -> "Java SE 20";
                case 63 -> "Java SE 19";
                case 62 -> "Java SE 18";
                case 61 -> "Java SE 17";
                case 60 -> "Java SE 16";
                case 59 -> "Java SE 15";
                case 58 -> "Java SE 14";
                case 57 -> "Java SE 13";
                case 56 -> "Java SE 12";
                case 55 -> "Java SE 11";
                case 54 -> "Java SE 10";
                case 53 -> "Java SE 9";
                case 52 -> "Java SE 8";
                case 51 -> "Java SE 7";
                case 50 -> "Java SE 6.0";
                case 49 -> "Java SE 5.0";
                case 48 -> "JDK 1.4";
                case 47 -> "JDK 1.3";
                case 46 -> "JDK 1.2";
                case 45 -> "JDK 1.1";
                default -> "JDK unknown for " + version;
            };
        }

        @Override
        public String toString() {
            StringBuffer constants = new StringBuffer();
            for (int i = 0; i < constantPool.length; i++) {
                constants
                    .append("#").append(i).append(" ")
                    .append("[").append(constantPool[i].type).append("] ")
                    .append(constantPool[i].constantValue.print(constantPool))
                    .append("\n");
            }

            return """
                Magic number: %s
                Minor Version: %s
                Major Version: %s
                Java version for Major Version: %s
                Constant Pool Count: %s
                Constant Pool:
                %s
                """.formatted(
                hex((byte) (magicNumber >> 24)) + " "
                    +hex((byte) (magicNumber >> 16)) + " "
                    +hex((byte) (magicNumber >> 8)) + " "
                    + hex((byte) magicNumber),
                minorVersion,
                majorVersion,
                version,
                constantPool.length,
                constants
            );
        }

        String hex(byte num) {
            char[] hexDigits = new char[2];
            hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
            hexDigits[1] = Character.forDigit(num & 0xF, 16);
            return "0x"+new String(hexDigits).toUpperCase();
        }
    }
}

