package utils;

import config.DbConfig;
import csv.resolver.info.ClassInfo;
import csv.resolver.info.ExtractorFromClass;
import csv.resolver.info.FunctionInfo;
import csv.resolver.taint.*;
import soot.SootClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringJoiner;

public class CsvUtils {
    public final static String ClassModifiers = "classModifiers";
    public final static String ExtendClass = "extendClass";
    public final static String ImplementInterfaces = "implementInterfaces";
    public final static String MethodModifiers = "methodModifiers";
    public final static String ParameterCount = "parameterCount";
    public final static String ParameterTypes = "parameterTypes";
    public final static String ReturnType = "returnType";

    private final static int PackageNameIndex = 0;
    private final static int ClassSignatureIndex = 1;
    private final static int ClassModifiersIndex = 2;
    private final static int ExtendClassIndex = 3;
    private final static int ImplementInterfacesIndex = 4;
    private final static int MethodSignatureIndex = 5;
    private final static int MethodModifiersIndex = 6;
    private final static int ParameterCountIndex = 7;
    private final static int ParameterTypesIndex = 8;
    private final static int ReturnTypeIndex = 9;
    private final static int KindIndex = 10;
    private final static int CategoryIndex = 11;

    public static String getMethodCSVFromRemoteClass(String className, boolean JDK) throws Exception {
        // print flag   ->    true: sout            false: write file
        // JDK flag     ->    true: CSV for JDK     false: CSV for Others
        ArrayList<FunctionInfo> functionInfos;
        functionInfos = new ExtractorFromClass(className).InitFromClass();

        StringBuilder stringBuilder = new StringBuilder();
        for (FunctionInfo functionInfo : functionInfos) {
            stringBuilder.append(generateMethodCSV(functionInfo, JDK)).append(StringsUtils.LF);
        }

        return stringBuilder.toString();
    }

    public static String getMethodCSVFromSootClass(ArrayList<FunctionInfo> functionInfos, boolean JDK) {
        StringBuilder stringBuilder = new StringBuilder();
        for (FunctionInfo functionInfo : functionInfos) {
            stringBuilder.append(generateMethodCSV(functionInfo, JDK)).append(StringsUtils.LF);
        }

        return stringBuilder.toString();
    }

    public static String getClassCSVFromSootClass(SootClass clazz, boolean JDK) {
        String ClassCategory = JDK ? SemCSV.JDKPackage : SemCSV.TPPPackage;

        return new StringJoiner(StringsUtils.colon)
                .add(clazz.getJavaPackageName())
                .add(clazz.getName())
                .add(ClassCategory).toString();
    }

    public static String generateMethodCSV(FunctionInfo functionInfo, boolean JDK) {
        ClassInfo classInfo = functionInfo.getClassInfo();
        String packageName = classInfo.getPackageName();
        String classSignature = StringsUtils.removePackageName(packageName, classInfo.getSignature());
        String classModifiers = classInfo.getClassModifiers();
        String extendClass = classInfo.getExtendClass();
        String implementInterfaces = classInfo.getImplementInterfaceClasses();
        String methodSignature = StringsUtils.removePackageName(packageName, functionInfo.getSignature());
        String methodModifiers = functionInfo.getModifiers();
        String parameterCount = functionInfo.getParameterCount();
        String parameterTypes = functionInfo.getParameterTypes();
        String returnType = functionInfo.getReturnType() == null ? StringsUtils.empty : functionInfo.getReturnType();
        String kind = StringsUtils.empty;   // Generate from Jar, this will be empty.
        String category = JDK ? SemCSV.JDKPackage : SemCSV.TPPPackage;

        return String.join(
                StringsUtils.colon,
                packageName,
                classSignature,
                classModifiers,
                extendClass,
                implementInterfaces,
                methodSignature,
                methodModifiers,
                parameterCount,
                parameterTypes,
                returnType,
                kind,
                category
        );
    }

    public static HashMap<String, String> getFunctionInfoMap(String CSV, String kind) {
        String[] CSVArray = CSV.split(StringsUtils.colon);
        HashMap<String, String> FunctionInfoMap = new HashMap<>();

        FunctionInfoMap.put(SemCSV.Namespace, CSVArray[PackageNameIndex]);
        FunctionInfoMap.put(SemCSV.Type, CSVArray[ClassSignatureIndex]);
        FunctionInfoMap.put(ClassModifiers, CSVArray[ClassModifiersIndex]);
        FunctionInfoMap.put(ExtendClass, CSVArray[ExtendClassIndex]);
        FunctionInfoMap.put(ImplementInterfaces, CSVArray[ImplementInterfacesIndex]);
        FunctionInfoMap.put(SemCSV.Name, CSVArray[MethodSignatureIndex]);
        FunctionInfoMap.put(MethodModifiers, CSVArray[MethodModifiersIndex]);
        FunctionInfoMap.put(ParameterCount, CSVArray[ParameterCountIndex]);
        FunctionInfoMap.put(ParameterTypes, CSVArray[ParameterTypesIndex]);
        FunctionInfoMap.put(ReturnType, CSVArray[ReturnTypeIndex]);
        FunctionInfoMap.put(SemCSV.Kind, kind);
        FunctionInfoMap.put(SemCSV.Category, CSVArray[CategoryIndex]);

        return FunctionInfoMap;
    }

    public static void getCustomTaintCsvFromCSVMap(HashMap<String, String> CSVMap, String calltype, HashSet<String> MethodSet) throws Exception {
        if (SemCSV.IsPorcessedBefore(CSVMap, MethodSet)) {
            return;
        }

        String name;
        if (CSVMap.get(SemCSV.Name).equals(CSVMap.get(SemCSV.Type))) {
            name = "<init>";
        } else {
            name = CSVMap.get(SemCSV.Name);
        }

        getCustomTaintCsv(
                CSVMap.get(SemCSV.Namespace),
                CSVMap.get(SemCSV.Type),
                name,
                calltype,
                StringsUtils.empty,
                CSVMap.get(SemCSV.Kind),
                StringsUtils.empty,
                false
        );
    }

    private static void getCustomTaintCsv(
            String FullMethod,
            String calltype,
            String returnType,
            String kind,
            String category,
            boolean print) throws Exception {
        int MethodDotIndex = FullMethod.lastIndexOf(StringsUtils.dot);
        String name = FullMethod.substring(MethodDotIndex + 1);
        String FullType = FullMethod.substring(0, MethodDotIndex);

        int TypeDotIndex = FullType.lastIndexOf(StringsUtils.dot);
        String namespace = FullType.substring(0, TypeDotIndex);
        String type = FullType.substring(TypeDotIndex + 1);
        if (name.equals(type)) {
            name = "<init>";
        }
        getCustomTaintCsv(namespace, type, name, calltype, returnType, kind, category, print);
    }

    private static void getCustomTaintCsv(
            String namespace,
            String type,
            String name,
            String calltype,
            String returnType,
            String kind,
            String category,
            boolean print) throws Exception {

        SeekVariants seekVariants = new SeekVariants();
        ArrayList<String> customCSVs = seekVariants.getCustomCSV(namespace, type, name);

        String customTaintCSV = StringsUtils.empty;
        if (customCSVs.size() == 0) {
            System.out.println("[?] The return type and category of the method " + String.join(StringsUtils.dot, namespace, type, name) + " is not found");
            customTaintCSV = String.join(
                    StringsUtils.colon,
                    namespace,
                    type,
                    name,
                    calltype,
                    returnType,
                    kind,
                    category);
        } else {
            for (String csv : customCSVs) {
                HashMap<String, String> CSVMap = new CustomCSV().getCSVMap(csv);

                customTaintCSV = String.join(
                        StringsUtils.colon,
                        namespace,
                        type,
                        name,
                        calltype,
                        CSVMap.get(ReturnType),
                        kind,
                        CSVMap.get(SemCSV.Category)
                );
            }
        }

        if (print) {
            System.out.println(customTaintCSV);
        }
        FilesUtils.WriteFile(FilesUtils.sinks_csv, customTaintCSV + StringsUtils.LF, true);
    }

    public static void main(String[] args) throws Exception {
        String FullMethod = "java.io.PrintStream.PrintStream";
        String calltype = "";
        String returnType = "";
        String kind = "";
        String category = SemCSV.TPPPackage;
        getCustomTaintCsv(FullMethod, calltype, returnType, kind, category, true);
    }
}