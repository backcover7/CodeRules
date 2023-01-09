package com.saucer.sast.lang.java.parser.filter.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Arrays;
import java.util.List;

public class RmiDeserialization extends FilterHelper<CtMethod<?>> {
    String category = "sink";
    String kind = "deserialization";
    String rule = "java.rmi:Remote:<Method>:deserialization";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtMethod<?> method) {
        List<String> PrimitiveType = Arrays.asList("int", "boolean", "short", "long", "byte", "char", "float", "double");
        if (method.getParameters().size() == 1
                && !PrimitiveType.contains(method.getParameters().get(0).getType().getQualifiedName())) {
            CtType<?> classtype = method.getParent(new TypeFilter<>(CtType.class));
            return classtype.isInterface() && isImplementInterface(classtype, "java.rmi.Remote");
        }
        return false;
    }
}
