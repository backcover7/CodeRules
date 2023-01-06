package com.saucer.sast.lang.java.parser.query;

import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.Filter;

public abstract class FilterHelper<T extends CtElement> implements Filter<T> {
    @Override
    public boolean matches(T t) {
        return false;
    }

    protected static CtElement getConstantVariable(CtElement obj) {
        if (obj instanceof CtVariableAccess) {
            return getConstantVariable(((CtVariableAccess<?>) obj).getVariable().getDeclaration().getDefaultExpression());
        } else {
            return obj;
        }
    }
}
