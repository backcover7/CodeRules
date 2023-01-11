package com.saucer.sast.lang.java.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public class JmsDeserialization extends FilterHelper<CtInvocation<?>> {
    public static String category = "sink";
    public static String kind = "deserialization";
    public static String rule = "javax.jms:MessageListener:onMessage:deserialization";
    public static boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation<?> invocation) {
        if (getSimpleName(invocation).equals("getObject")) {
            CtMethod<?> callee = invocation.getParent(new TypeFilter<>(CtMethod.class));
            if (callee.getSimpleName().equals("onMessage")
                    && callee.getParameters().size() == 1
                    && callee.getParameters().get(0).getType().getQualifiedName().equals("javax.jms.Message")) {
                CtType<?> classtype = invocation.getParent(new TypeFilter<>(CtType.class));
                return isImplementInterface(classtype, "javax.jms.MessageListener");
            }
        }
        return false;
    }
}