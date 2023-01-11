package com.saucer.sast.lang.java.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;

import java.util.List;

public class PaddingOracle extends FilterHelper<CtInvocation<?>> {
    public static String category = "sink";
    public static String kind = "crypto";
    public static String rule = "javax.crypto:Cipher:getInstance:crypto";
    public static boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        if (getSimpleName(invocation).equals("getInstance")
                && getQualifiedType(invocation.getTarget()).equals("javax.crypto.Cipher")) {
            List<CtExpression<?>> arguments = invocation.getArguments();
            return getElementStringValue(getArgument(arguments, 0)).contains("CBC");
        }
        return false;
    }
}