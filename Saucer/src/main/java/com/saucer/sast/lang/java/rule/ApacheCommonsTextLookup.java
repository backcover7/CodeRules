package com.saucer.sast.lang.java.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

import java.util.List;

public class ApacheCommonsTextLookup extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "el";
    String rule = "org.apache.commons.text:StringSubstitutor:replace:el";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        if (getSimpleName(invocation).equals("replace")) {
            CtExpression<?> target = invocation.getTarget();
            if (getQualifiedType(target).equals("org.apache.commons.text.StringSubstitutor")) {
                if (getSimpleName(target).equals("createInterpolator")) {
                    return true;
                } else {
                    List<CtExpression<?>> arguments = getArguments(target);
                    if (arguments.size() == 1) {
                        CtElement argumentElement = getArgument(target, 0);
                        return getSimpleName(argumentElement).equals("interpolatorStringLookup");
                    }
                }
            }
        }
        return false;
    }
}