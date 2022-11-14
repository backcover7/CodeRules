package csv.resolver.info;

import utils.StringsUtils;

public class FunctionInfo {
    private ClassInfo classInfo;
    private String signature;
    private String modifiers;
    private String parameterCount;
    private String parameterTypes;
    private String returnType;

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = StringsUtils.resolveModifers(modifiers);
    }

    public String getParameterCount() {
        return parameterCount;
    }

    public void setParameterCount(String parameterCount) {
        this.parameterCount = parameterCount;
    }

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
}
