# javap -v -c "jar:file:./SimpleJavaApp-1.0-SNAPSHOT.jar\!/com/example/Main.class"
java -javaagent:./build/libs/JavaAgentExperiment-1.0-SNAPSHOT.jar -jar SimpleJavaApp-1.0-SNAPSHOT.jar
