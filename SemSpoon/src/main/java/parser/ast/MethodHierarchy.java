package parser.ast;

import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtParameterImpl;

import java.util.List;
import java.util.Set;

public class MethodHierarchy {
    public String FindConstructorDefinition(CtType<?> clazz, List<CtTypeReference<?>> parameters) {
        if (clazz.getSuperclass() != null) {
            CtClass<?> superClass = (CtClass<?>) clazz.getSuperclass().getTypeDeclaration();
            Set<? extends CtConstructor<?>> ctConstructors = superClass.getConstructors();
            for (CtConstructor<?> ctConstructor:ctConstructors) {
                List<CtParameter<?>> SuperDeclarationConstructorParameters = ctConstructor.getParameters();
                if (SuperDeclarationConstructorParameters.size() == parameters.size()) {
                    for (CtTypeReference<?> param : parameters) {
                        int paramIndex = parameters.indexOf(param);
                        Object a = SuperDeclarationConstructorParameters.get(paramIndex);
                        if (!param.getQualifiedName().equals(((CtParameterImpl) a).getType().getTypeDeclaration().getQualifiedName())) {
                            break;
                        }
                        FindConstructorDefinition(superClass, parameters);
                    }
                }
            }
        }
        return clazz.getQualifiedName();
    }

    public String FindMethodDefinition(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        CtType<?> ctMethodSuperclassDeclaration = ExtendsFromSuperclass(clazz, methodName, parameters);
        if (ctMethodSuperclassDeclaration != null) {
            if (ctMethodSuperclassDeclaration.getSuperInterfaces().size() != 0) {
                return ImplemenstFromInterface(ctMethodSuperclassDeclaration, methodName, parameters);
            }
        }
        return ImplemenstFromInterface(clazz, methodName, parameters);
    }

    private CtType<?> ExtendsFromSuperclass(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        if (!clazz.isInterface() && !clazz.isAbstract()) {
            CtTypeReference<?> superClass = clazz.getSuperclass();
            Set<CtMethod<?>> ctMethods = superClass.getTypeDeclaration().getAllMethods();
            for (CtMethod<?> ctMethod : ctMethods) {
                if (ctMethod.getSimpleName().equals(methodName)) {
                    List<CtParameter<?>> SuperDeclarationMethodParameters = ctMethod.getParameters();
                    if (SuperDeclarationMethodParameters.size() == parameters.size()) {
                        for (CtTypeReference<?> param : parameters) {
                            int paramIndex = parameters.indexOf(param);
                            Object a = SuperDeclarationMethodParameters.get(paramIndex);
                            if (!param.getQualifiedName().equals(((CtParameterImpl) a).getType().getTypeDeclaration().getQualifiedName())) {
                                break;
                            }
                            ExtendsFromSuperclass(superClass.getTypeDeclaration(), methodName, parameters);
                        }
                    }
                    return ctMethod.getDeclaringType();
                }
            }
        }
        return clazz.getDeclaringType();
    }

    private String ImplemenstFromInterface(CtType<?> clazz, String methodName, List<CtTypeReference<?>> parameters) {
        Set<CtTypeReference<?>> interfaces = clazz.getSuperInterfaces();
        for (CtTypeReference<?> interfaceType : interfaces) {
            Set<CtMethod<?>> interfaceTypeMethods = interfaceType.getTypeDeclaration().getAllMethods();
            for (CtMethod<?> interfaceTypeMethod : interfaceTypeMethods) {
                if (interfaceTypeMethod.getSimpleName().equals(methodName)) {
                    List<CtParameter<?>> InterfaceDeclarationMethodParameters = interfaceTypeMethod.getParameters();
                    if (InterfaceDeclarationMethodParameters.size() == parameters.size()) {
                        for (CtTypeReference<?> param : parameters) {
                            int paramIndex = parameters.indexOf(param);
                            Object a = InterfaceDeclarationMethodParameters.get(paramIndex);
                            if (!param.getQualifiedName().equals(((CtParameterImpl) a).getType().getTypeDeclaration().getQualifiedName())) {
                                break;
                            }
                            ExtendsFromSuperclass(interfaceType.getTypeDeclaration(), methodName, parameters);
                        }
                    }
                    return interfaceType.getQualifiedName();
                }
            }
        }
        return null;
    }
}
