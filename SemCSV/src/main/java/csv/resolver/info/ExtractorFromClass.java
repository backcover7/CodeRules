package csv.resolver.info;

import utils.StringsUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class ExtractorFromClass {
    public String className;

    public ClassInfo classInfo = new ClassInfo();

    public ExtractorFromClass(String className) {
        this.className = className;
    }

    public ArrayList<FunctionInfo> InitFromClass() {
        return getClassFunctionInfo(getClassObject(className));
    }

    public ArrayList<FunctionInfo> getClassFunctionInfo(Class<?> clazz) {
        if(clazz.getPackage() != null) {
            classInfo.setPackageName(clazz.getPackage().getName());
        } else {
            classInfo.setPackageName(StringsUtils.empty);
        }
        classInfo.setSignature(clazz.getName());
        classInfo.setClassModifiers(Modifier.toString(clazz.getModifiers()));
        if(!clazz.isInterface()) {
            classInfo.setExtendClass(
                    StringsUtils.nullClass2Empty(clazz.getSuperclass()));
        } else {
            classInfo.setExtendClass(StringsUtils.empty);
        }
        classInfo.setImplementInterfaceClasses(
                StringsUtils.concatClassArray(clazz.getInterfaces()));
        ArrayList<FunctionInfo> functionInfos = new ArrayList<>();
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            functionInfos = getClassConstructorsInfo(constructors);
            Method[] methods = clazz.getDeclaredMethods();
            functionInfos.addAll(getClassMethodsInfo(methods));
        } catch (NoClassDefFoundError e) {
            System.out.println("[!] Error when resolving the class " + className);
//            e.printStackTrace();
        }

        return functionInfos;
    }

    // TODO: Add isOverride()
    private ArrayList<FunctionInfo> getClassConstructorsInfo(Constructor<?>[] constructors) {
        ArrayList<FunctionInfo> constructorInfos = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            ConstructorInfo constructorInfo = new ConstructorInfo();
            constructorInfo.setClassInfo(classInfo);
            constructorInfo.setSignature(constructor.getName());
            constructorInfo.setModifiers(Modifier.toString(constructor.getModifiers()));
            constructorInfo.setParameterCount(String.valueOf(constructor.getParameterCount()));
            constructorInfo.setParameterTypes(
                    StringsUtils.concatClassArray(constructor.getParameterTypes()));
            constructorInfos.add(constructorInfo);
        }
        return constructorInfos;
    }

    private ArrayList<FunctionInfo> getClassMethodsInfo(Method[] methods) {
        ArrayList<FunctionInfo> methodInfos = new ArrayList<>();
        for (Method method : methods) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setClassInfo(classInfo);
            methodInfo.setSignature(method.getName());
            methodInfo.setModifiers(Modifier.toString(method.getModifiers()));
            methodInfo.setParameterCount(String.valueOf(method.getParameterCount()));
            methodInfo.setParameterTypes(
                    StringsUtils.concatClassArray(method.getParameterTypes()));
            methodInfo.setReturnType(method.getReturnType().getName());
            methodInfos.add(methodInfo);
        }
        return methodInfos;
    }

    private Class<?> getClassObject(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
