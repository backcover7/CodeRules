package com.saucer.sast.lang.java.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtInvocation;

public class SqliExternalConfig extends FilterHelper<CtInvocation<?>> {
    public static String category = "sink";
    public static String kind = "sqli";
    public static String rule = "java.sql:Connection:setCatalog:sqli";
    public static boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation<?> invocation) {
        if (getSimpleName(invocation).equals("setCatalog")
                && getQualifiedType(invocation.getTarget()).equals("java.sql.Connection")) {
            return getElementStringValue(getArgument(invocation, 0)).equals("EXTERNAL_CONFIG_CONTROL");
        }
        return false;
    }
}
