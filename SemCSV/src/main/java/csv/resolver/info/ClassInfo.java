package csv.resolver.info;

import utils.StringsUtils;

public class ClassInfo {
    private String packageName;
    private String signature;
    private String classModifiers;
    private String extendClass;
    private String implementInterfaceClasses;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getClassModifiers() {
        return classModifiers;
    }

    public void setClassModifiers(String classModifiers) {
        this.classModifiers = StringsUtils.resolveModifers(classModifiers);
    }

    public String getExtendClass() {
        return extendClass;
    }

    public void setExtendClass(String extendClass) {
        this.extendClass = extendClass;
    }

    public String getImplementInterfaceClasses() {
        return implementInterfaceClasses;
    }

    public void setImplementInterfaceClasses(String implementInterfaceClasses) {
        this.implementInterfaceClasses = implementInterfaceClasses;
    }
}
