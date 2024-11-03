# JavaAgentDisassembler

A simple agent that tries to disassemble the byte code for a given class.  

This is both a demonstration of how agents work and a playground for experiments.  

All disassembling is contained on the repo, no external library is used.  

# Usage

```
java -jar javaAgentDisassembler.jar <class_file_path>
java -javaagent:javaAgentDisassembler.jar=MyClass -jar app.jar
```

# References

[The class specs](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html)  
