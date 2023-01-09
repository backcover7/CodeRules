package com.saucer.sast.lang.java.parser.filter;

import spoon.reflect.code.CtInvocation;

public class TemplateFilter extends FilterHelper<CtInvocation<?>> {
    String category = "";
    String kind = "";
    String rule = "";
    /*
     * Check RuleNode class for all RuleFlag
     *
     * private boolean isWebAnnotationSource;
     * private boolean isNativeGadgetSource;
     * private boolean isJsonGadgetSource;
     * private boolean isWebInvocationSource;
     * private boolean isInvocationSink;
     * private boolean isAnnotationSink;
     */
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        return false;
    }
}
