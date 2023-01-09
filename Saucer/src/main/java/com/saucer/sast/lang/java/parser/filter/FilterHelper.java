package com.saucer.sast.lang.java.parser.filter;

import com.saucer.sast.utils.CharUtils;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class FilterHelper<T extends CtElement> implements Filter<T> {
    private static CtElement getConstantVariable(CtElement elem) {
        if (elem instanceof CtVariableAccess) {
            return getConstantVariable(((CtVariableAccess<?>) elem).getVariable().getDeclaration().getDefaultExpression());
        } else {
            return elem;
        }
    }

    protected static String getQualifiedType(CtElement elem) {
        CtElement ctElement = getConstantVariable(elem);
        if (ctElement instanceof CtInvocation) {
            return ((CtInvocation<?>) ctElement).getType().getQualifiedName();
        } else if (ctElement instanceof CtConstructorCall) {
            return ((CtConstructorCall<?>) ctElement).getType().getQualifiedName();
        } else if (ctElement instanceof CtTypeAccess) {
            return ((CtTypeAccess<?>) ctElement).getAccessedType().getQualifiedName();
        } else {
            return CharUtils.empty;
        }
    }

    protected static String getSimpleName(CtElement elem) {
        CtElement ctElement = getConstantVariable(elem);
        if (ctElement instanceof CtInvocation) {
            return ((CtInvocation<?>) ctElement).getExecutable().getSimpleName();
        } else if (ctElement instanceof CtConstructorCall) {
            return ((CtConstructorCall<?>) ctElement).getExecutable().getSimpleName();
        } else if (ctElement instanceof CtTypeAccess) {
            return ((CtTypeAccess<?>) ctElement).getAccessedType().getSimpleName();
        } else {
            return CharUtils.empty;
        }
    }

    protected static List<CtExpression<?>> getArguments(CtElement elem) {
        CtElement ctElement = getConstantVariable(elem);
        if (ctElement instanceof CtAbstractInvocation) {
            return ((CtAbstractInvocation<?>) ctElement).getArguments();
        }
        return new ArrayList<>();
    }

    protected static CtElement getArgument(CtElement elem, int index) {
        return getConstantVariable(getArguments(elem).get(index));
    }

    protected static CtElement getArgument(List<CtExpression<?>> arguments, int index) {
        return getConstantVariable(arguments.get(index));
    }

    protected static String getElementStringValue(CtElement elem) {
        CtElement ctElement = getConstantVariable(elem);
        if (ctElement instanceof CtLiteral) {
            CtLiteral<?> transformationLiteral = (CtLiteral<?>) ctElement;
            return transformationLiteral.getValue().toString();
        }
        return CharUtils.empty;
    }

    protected static boolean isImplementInterface(CtType<?> classtype, String interfaceName) {
        Set<CtTypeReference<?>> interfaces = classtype.getSuperInterfaces();
        for (CtTypeReference<?> eachInterface : interfaces) {
            if (eachInterface.getQualifiedName().equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }
}