package com.saucer.sast.lang.java.parser.filter;

import spoon.reflect.declaration.CtElement;

public class FilterTemplate extends FilterHelper<CtElement> {
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
    public boolean matches(CtElement element) {
        return false;
    }
}
