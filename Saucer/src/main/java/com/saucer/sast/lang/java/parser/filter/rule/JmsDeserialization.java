package com.saucer.sast.lang.java.parser.filter.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Set;

public class JmsDeserialization extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "deserialization";
    String rule = "javax.jms:MessageListener:onMessage:deserialization";
    boolean isInvocationSink = true;

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
