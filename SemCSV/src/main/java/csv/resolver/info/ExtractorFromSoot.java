package csv.resolver.info;

import soot.*;
import utils.StringsUtils;

import java.lang.reflect.Modifier;
import java.util.*;

public class ExtractorFromSoot {
    public SootClass clazz;

    public ClassInfo classInfo = new ClassInfo();

    public ExtractorFromSoot (SootClass sootClass) {
        this.clazz = sootClass;
    }

    public ArrayList<FunctionInfo> InitFromSoot() {
        return getClassFunctionInfo(clazz);
    }

    public ArrayList<FunctionInfo> getClassFunctionInfo(SootClass clazz) {
        if(clazz.getJavaPackageName() != null) {
            classInfo.setPackageName(clazz.getJavaPackageName());
        } else {
            classInfo.setPackageName(StringsUtils.empty);
        }
        classInfo.setSignature(clazz.getName());
        classInfo.setClassModifiers(java.lang.reflect.Modifier.toString(clazz.getModifiers()));
        if(!clazz.isInterface()) {
            try {
                classInfo.setExtendClass(clazz.getSuperclass().toString());
            } catch (Exception e) {
                classInfo.setExtendClass(StringsUtils.empty);
            }

        } else {
            classInfo.setExtendClass(StringsUtils.empty);
        }
        classInfo.setImplementInterfaceClasses(
                StringsUtils.concatObjectArray(clazz.getInterfaces().toArray()));
        ArrayList<FunctionInfo> functionInfos = new ArrayList<>();
        try {
            List<SootMethod> methods = clazz.getMethods();
            functionInfos.addAll(getClassMethodsInfo(methods));
        } catch (NoClassDefFoundError e) {
            System.out.println("[!] Error when resolving the class " + clazz.getName());
//            e.printStackTrace();
        }

        return functionInfos;
    }

    private ArrayList<FunctionInfo> getClassMethodsInfo(List<SootMethod> methods) {
        ArrayList<FunctionInfo> methodInfos = new ArrayList<>();
        for (SootMethod method : methods) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setClassInfo(classInfo);
            methodInfo.setSignature(method.getName());
            methodInfo.setModifiers(Modifier.toString(method.getModifiers()));
            methodInfo.setParameterCount(String.valueOf(method.getParameterCount()));
            methodInfo.setParameterTypes(
                    StringsUtils.concatSootTypeArray(method.getParameterTypes()));
            methodInfo.setReturnType(method.getReturnType().toString());
            methodInfos.add(methodInfo);
        }
        return methodInfos;
    }
}
