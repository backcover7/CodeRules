package com.saucer.sast.lang.java.parser.filter.rule;

import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtInvocation;

public class SqliExternalConfig extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "sqli";
    String rule = "java.sql:Connection:setCatalog:sqli";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation<?> invocation) {
        if (getSimpleName(invocation).equals("setCatalog")
                && getQualifiedType(invocation.getTarget()).equals("java.sql.Connection")) {
            return getElementStringValue(getArgument(invocation, 0)).equals("EXTERNAL_CONFIG_CONTROL");
        }
        return false;
    }
}
