package org.example;

import java.lang.instrument.Instrumentation;

public class JavaAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) throws InstantiationException {
        InterceptingClassTransformer interceptingClassTransformer = new InterceptingClassTransformer();
        interceptingClassTransformer.init();
        instrumentation.addTransformer(interceptingClassTransformer);
    }
}

