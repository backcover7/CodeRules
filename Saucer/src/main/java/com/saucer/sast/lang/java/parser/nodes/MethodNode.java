package com.saucer.sast.lang.java.parser.nodes;

import com.contrastsecurity.sarif.Location;
import com.saucer.sast.utils.CharUtils;

public class MethodNode {
    private ClassNode fullClasstype;
    private String name;
    private String returntype;
    private String signature;
    private String sourceCode;
    private Location methodLocation;  // todo, what is logisticlocation?

    private boolean isAnnotationFlag;
    private boolean isConstructorFlag;
    private boolean isMethodFlag;

    private boolean isWebAnnotationSource;
    private boolean isNativeGadgetSource;
    private boolean isJsonGadgetSource;
    private boolean isWebInvocationSource;
    private boolean isSinkInvocation;
    private boolean isSinkAnnotation;  // todo
    private boolean isSourcePropagator;
    private boolean isSinkPropagator;
    private int MethodID;

    public static String METHOD = "methodname";
    public static String RETURNTYPE = "returntype";
    public static String SIGNATURE = "signature";
    public static String SOURCECODE = "sourcecode";
    public static String METHODLOCATION = "location";

    public static String ISANNOTATION = "isAnnotation";
    public static String ISCONSTRUCTOR = "isConstructor";
    public static String ISMETHOD = "isMethod";
    public static String ISWEBANNOTATIONSOURCE = "isWebAnnotationSource";
    public static String ISNATIVEGADGETSOURCE = "isNativeGadgetSource";
    public static String ISJSONGADGETSOURCE = "isJsonGadgetSource";
    public static String ISWEBINVOCATIONSOURCE = "isWebInvocationSource";
    public static String ISSINKINVOCATION = "isSinkInvocation";
    public static String ISSINKANNOTATION = "isSinkAnnotation";
    public static String ISSOURCEPROPAGATOR = "isSourcePropagator";
    public static String ISSINKPROPAGATOR = "isSinkPropagator";

    public ClassNode getFullClasstype() {
        return fullClasstype;
    }

    public void setFullClasstype(ClassNode fullClasstype) {
        this.fullClasstype = fullClasstype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturntype() {
        return returntype;
    }

    public void setReturntype(String returntype) {
        this.returntype = returntype;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Location getMethodLocation() {
        return methodLocation;
    }

    public void setMethodLocation(Location methodLocation) {
        this.methodLocation = methodLocation;
    }

    public boolean isAnnotationFlag() {
        return isAnnotationFlag;
    }

    public void setAnnotationFlag(boolean annotationFlag) {
        isAnnotationFlag = annotationFlag;
    }

    public boolean isConstructorFlag() {
        return isConstructorFlag;
    }

    public void setConstructorFlag(boolean constructorFlag) {
        isConstructorFlag = constructorFlag;
    }

    public boolean isMethodFlag() {
        return isMethodFlag;
    }

    public void setMethodFlag(boolean methodFlag) {
        isMethodFlag = methodFlag;
    }

    public boolean isWebAnnotationSource() {
        return isWebAnnotationSource;
    }

    public void setWebAnnotationSource(boolean webAnnotationSource) {
        isWebAnnotationSource = webAnnotationSource;
    }

    public boolean isNativeGadgetSource() {
        return isNativeGadgetSource;
    }

    public void setNativeGadgetSource(boolean nativeGadgetSource) {
        isNativeGadgetSource = nativeGadgetSource;
    }

    public boolean isJsonGadgetSource() {
        return isJsonGadgetSource;
    }

    public void setJsonGadgetSource(boolean jsonGadgetSource) {
        isJsonGadgetSource = jsonGadgetSource;
    }

    public boolean isSourcePropagator() {
        return isSourcePropagator;
    }

    public void setSourcePropagator(boolean sourcePropagator) {
        isSourcePropagator = sourcePropagator;
    }

    public boolean isWebInvocationSource() {
        return isWebInvocationSource;
    }

    public void setWebInvocationSource(boolean webInvocationSource) {
        isWebInvocationSource = webInvocationSource;
    }

    public boolean isSinkInvocation() {
        return isSinkInvocation;
    }

    public void setSinkInvocation(boolean sinkInvocation) {
        isSinkInvocation = sinkInvocation;
    }

    public boolean isSinkAnnotation() {
        return isSinkAnnotation;
    }

    public void setSinkAnnotation(boolean sinkAnnotation) {
        isSinkAnnotation = sinkAnnotation;
    }

    public boolean isSinkPropagator() {
        return isSinkPropagator;
    }

    public void setSinkPropagator(boolean sinkPropagator) {
        isSinkPropagator = sinkPropagator;
    }

    public int getMethodID() {
        return MethodID;
    }

    public void setMethodID(int methodID) {
        MethodID = methodID;
    }

    public void setAnnotationFlagTrue() {
        isAnnotationFlag = true;
        isConstructorFlag = false;
        isMethodFlag = false;
    }

    public void setConstructorFlagTrue() {
        isAnnotationFlag = false;
        isConstructorFlag = true;
        isMethodFlag = false;
    }

    public void setMedthodFlagTrue() {
        isAnnotationFlag = false;
        isConstructorFlag = false;
        isMethodFlag = true;
    }

    public String getFullQualifiedName() {
        return String.join(
                CharUtils.dot,
                fullClasstype.getFullQualifiedName(),
                name
        );
    }
}
