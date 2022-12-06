package com.saucer.sast.lang.java.parser.core;

import com.saucer.sast.lang.java.config.SpoonConfig;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtParameterImpl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodHierarchy {
    private final HashSet<String> methodSet = new HashSet<>();

    public HashSet<String> getMethodSet() {
        return methodSet;
    }

    public void FindMethodDefinition(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        CtType<?> ctMethodSuperclassDeclaration = ExtendsFromSuperclass(clazz, methodName, parameters);
        if (ctMethodSuperclassDeclaration != null) {
            if (ctMethodSuperclassDeclaration.getSuperInterfaces().size() != 0) {
                ImplementsFromInterface(ctMethodSuperclassDeclaration, methodName, parameters);
            }
        }
        ImplementsFromInterface(clazz, methodName, parameters);

        // When superclass is null and the clazz has no superinterface
        // It still has a superclass which is java.lang.Object
        // Do not need to implement any logic, coz there's only one node in csv: java.lang:Object:finalize:gadget
        if (methodName.equals("finalize")) {
            methodSet.add("java.lang.Object");
        }
    }

    private CtType<?> ExtendsFromSuperclass(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        CtTypeReference<?> superClass = clazz.getSuperclass();
        if (!clazz.isInterface() && !clazz.isAbstract() && superClass != null) {
            Set<CtMethod<?>> ctMethods = superClass.getTypeDeclaration().getAllMethods();
            for (CtMethod<?> ctMethod : ctMethods) {
                if (ctMethod.getSimpleName().equals(methodName)) {
                    List<CtParameter<?>> SuperDeclarationMethodParameters = ctMethod.getParameters();
                    if (SuperDeclarationMethodParameters.size() == parameters.size()) {
                        for (CtTypeReference<?> param : parameters) {
                            int paramIndex = parameters.indexOf(param);
                            Object a = SuperDeclarationMethodParameters.get(paramIndex);
                            if (!param.getQualifiedName().equals(((CtParameterImpl<?>) a).getType().getTypeDeclaration().getQualifiedName())) {
                                break;
                            }
                            ExtendsFromSuperclass(superClass.getTypeDeclaration(), methodName, parameters);
                        }
                    }
                    methodSet.add(ctMethod.getDeclaringType().getQualifiedName());
                    return ctMethod.getDeclaringType();
                }
            }
        }
        methodSet.add(clazz.getQualifiedName());
        return clazz;
    }

    private void ImplementsFromInterface(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        Set<CtTypeReference<?>> interfaces = clazz.getSuperInterfaces();
        for (CtTypeReference<?> interfaceType : interfaces) {
            Set<CtMethod<?>> interfaceTypeMethods = interfaceType.getTypeDeclaration().getAllMethods();
            interfaceTypeMethods.parallelStream().forEach(interfaceTypeMethod -> {
                if (interfaceTypeMethod.getSimpleName().equals(methodName)) {
                    List<CtParameter<?>> InterfaceDeclarationMethodParameters = interfaceTypeMethod.getParameters();
                    if (InterfaceDeclarationMethodParameters.size() == parameters.size()) {
                        for (CtTypeReference<?> param : parameters) {
                            int paramIndex = parameters.indexOf(param);
                            Object a = InterfaceDeclarationMethodParameters.get(paramIndex);
                            if (!param.getQualifiedName().equals(((CtParameterImpl<?>) a).getType().getTypeDeclaration().getQualifiedName())) {
                                break;
                            }
                            ImplementsFromInterface(interfaceType.getTypeDeclaration(), methodName, parameters);
                        }
                    }
                    methodSet.add(interfaceType.getQualifiedName());
                }
            });
        }
    }

    public static boolean isSerializable(CtType<?> clazz) {
        CtTypeReference<Object> Serializable = SpoonConfig.launcher.getFactory()
                .createCtTypeReference(java.io.Serializable.class);
        return clazz.isSubtypeOf(Serializable);
    }

    public static boolean isExternalizable(CtType<?> clazz) {
        CtTypeReference<Object> Externalizable = SpoonConfig.launcher.getFactory()
                .createCtTypeReference(java.io.Externalizable.class);
        return clazz.isSubtypeOf(Externalizable);
    }
}

