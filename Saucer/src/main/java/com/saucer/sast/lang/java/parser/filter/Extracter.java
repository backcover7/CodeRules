package com.saucer.sast.lang.java.parser.filter;

import com.contrastsecurity.sarif.Location;
import com.saucer.sast.Main;
import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.filter.rule.JmsDeserialization;
import com.saucer.sast.lang.java.parser.filter.rule.RmiDeserialization;
import com.saucer.sast.lang.java.parser.nodes.*;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.FileUtils;
import com.saucer.sast.utils.SpoonUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Extracter {
    public final static String RuleDirectory = Paths.get(Main.rule).toAbsolutePath().normalize().toString();

    public static class ExtractRuleObject{
        private RuleNode ruleNode;
        private String ruleFlag;
        private Filter<?> filter;

        private List<Object> elems;

        public RuleNode getRuleNode() {
            return ruleNode;
        }

        public void setRuleNode(RuleNode ruleNode) {
            this.ruleNode = ruleNode;
        }

        public String getRuleFlag() {
            return ruleFlag;
        }

        public void setRuleFlag(String ruleFlag) {
            this.ruleFlag = ruleFlag;
        }

        public Filter<?> getFilter() {
            return filter;
        }

        public void setFilter(Filter<?> filter) {
            this.filter = filter;
        }

        public List<Object> getElems() {
            return elems;
        }

        public void setElems(List<Object> elems) {
            this.elems = elems;
        }
    }

    // https://spoon.gforge.inria.fr/filter.html
    private ExtractRuleObject FilterElements(String rule) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(rule);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        try {
            ExtractRuleObject extractRuleObject = loadExtensibleRule(model);
            extractRuleObject.setElems(SpoonConfig.model.getRootPackage().filterChildren(
                    extractRuleObject.getFilter()
//                    new TemplateFilter()
//                    new RmiDeserialization()
            ).list());
            return extractRuleObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ExtractRuleObject loadExtensibleRule(CtModel model) {
        List<CtType<?>> elements = model.getElements(new TypeFilter<>(CtType.class));

        try {
            for (CtType<?> filter : elements) {
                CtTypeReference<?> superClass = filter.getSuperclass();
                if (superClass.getActualClass() == FilterHelper.class) {
                    List<CtMethod<?>> matchesMethods = filter.getMethodsByName("matches");
                    if (matchesMethods.size() == 1) {
                        CtMethod<?> matches = matchesMethods.get(0);
                        if (matches.isPublic() &&
                                matches.getType().getQualifiedName().equals("boolean")) {
                            List<CtParameter<?>> parameters = matches.getParameters();
                            if (parameters.size() == 1) {
                                RuleNode ruleNode = new RuleNode();
                                ruleNode.setCategory(filter.getField(RuleNode.CATEGORY).getAssignment().toString());
                                ruleNode.setKind(filter.getField(RuleNode.KIND).getAssignment().toString());
                                ruleNode.setRule(filter.getField(RuleNode.RULE).getAssignment().toString());

                                Filter<?> filterObj = (Filter<?>) ((CtClass<?>) filter).newInstance();

                                Stream<CtField<?>> RuleFlagFieldStream = filter.getFields().stream().filter(
                                        field -> field.getSimpleName().startsWith("is"));
                                String RuleFlag =RuleFlagFieldStream.findFirst().get().getSimpleName();

                                try {
                                    RuleNode.class.getDeclaredField(RuleFlag);
                                } catch (Exception e) {
                                    System.err.println("[!] Please recheck the flag field in your rule.");
                                    System.exit(1);
                                }

                                ExtractRuleObject extractRuleObject = new ExtractRuleObject();
                                extractRuleObject.setRuleNode(ruleNode);
                                extractRuleObject.setFilter(filterObj);
                                extractRuleObject.setRuleFlag(RuleFlag);
                                return extractRuleObject;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[!] Wrong format of Filter rule! Please check your rule again.");
            System.exit(1);
        }

        return null;
    }

    public void FilterAndUpdate() {
        ArrayList<String> rules = FileUtils.getExtensionFiles(RuleDirectory, CharUtils.JavaExtension, true);
        for (String rule : rules) {
            FilterAndUpdate(rule);
        }
    }

    // Todo
    public void FilterAndUpdate(String rule) {
        ExtractRuleObject extractRuleObject = FilterElements(rule);
        List<Object> elems = extractRuleObject.getElems();
        for (Object elem: elems) {
            Location location = new Location();
            if (elem instanceof CtInvocation) {
                CtInvocation<?> invocation = (CtInvocation<?>) elem;
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(invocation.getExecutable().getDeclaringType().getTopLevelType().getPackage().getQualifiedName());
                classNode.setName(invocation.getExecutable().getDeclaringType().getSimpleName());
                SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
                simpleMethodNode.setFullClasstype(classNode);
                simpleMethodNode.setName(invocation.getExecutable().getSimpleName());
                MethodNode methodNode = new MethodNode();
                methodNode.setSimpleMethodNode(simpleMethodNode);
                SourceNode sourceNode = new SourceNode();
                sourceNode.setMethodNode(methodNode);
                InvocationNode invocationMethod = new InvocationNode();
                invocationMethod.setSourceNode(sourceNode);
                invocationMethod.setSnippet(invocation.getOriginalSourceFragment().getSourceCode());
                location = SpoonUtils.ConvertPosition2Location(invocationMethod, invocation.getOriginalSourceFragment().getSourcePosition());
            } else if (elem instanceof CtConstructorCall) {
                CtConstructorCall<?> constructorCall = (CtConstructorCall<?>) elem;
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(constructorCall.getExecutable().getDeclaringType().getTopLevelType().getPackage().getQualifiedName());
                classNode.setNamespace(constructorCall.getExecutable().getDeclaringType().getSimpleName());
                SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
                simpleMethodNode.setFullClasstype(classNode);
                simpleMethodNode.setName("<init>");
                MethodNode methodNode = new MethodNode();
                methodNode.setSimpleMethodNode(simpleMethodNode);
                SourceNode sourceNode = new SourceNode();
                sourceNode.setMethodNode(methodNode);
                InvocationNode invocationConstructor = new InvocationNode();
                invocationConstructor.setSourceNode(sourceNode);
                invocationConstructor.setSnippet(constructorCall.getOriginalSourceFragment().getSourceCode());
                location = SpoonUtils.ConvertPosition2Location(invocationConstructor, constructorCall.getOriginalSourceFragment().getSourcePosition());
            } else if (elem instanceof CtAnnotation) {
                CtAnnotation<?> annotation = (CtAnnotation<?>) elem;
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(annotation.getAnnotationType().getPackage().getQualifiedName());
                classNode.setNamespace(annotation.getAnnotationType().getSimpleName());
                SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
                simpleMethodNode.setFullClasstype(classNode);
                simpleMethodNode.setName(null);
                MethodNode methodNode = new MethodNode();
                methodNode.setSimpleMethodNode(simpleMethodNode);
                SourceNode sourceNode = new SourceNode();
                sourceNode.setMethodNode(methodNode);
                InvocationNode invocationAnnotation = new InvocationNode();
                invocationAnnotation.setSnippet(annotation.getOriginalSourceFragment().getSourceCode());
                invocationAnnotation.setSourceNode(sourceNode);
                location = SpoonUtils.ConvertPosition2Location(invocationAnnotation, annotation.getOriginalSourceFragment().getSourcePosition());
                int ParentMethodID = DbUtils.QueryAnnotationParentMethodID(invocationAnnotation);
                if (ParentMethodID != -1) {
                    DbUtils.UpdateParentMethodAsWebAnnoationSource(ParentMethodID, extractRuleObject.getRuleNode(), extractRuleObject.getRuleFlag());
                } else {
                    System.err.println("[!] Error: Issue in processing annotation in rule");
                    System.exit(1);
                }
            } else if (elem instanceof CtMethod) {
                CtMethod<?> method = (CtMethod<?>) elem;
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(method.getTopLevelType().getPackage().getQualifiedName());
                classNode.setName(method.getDeclaringType().getSimpleName());
                SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
                simpleMethodNode.setFullClasstype(classNode);
                simpleMethodNode.setName(method.getSimpleName());
                MethodNode methodNode = new MethodNode();
                methodNode.setSimpleMethodNode(simpleMethodNode);
                methodNode.setReturntype(method.getType().getQualifiedName());
                methodNode.setSignature(method.getSignature());
                methodNode.setSourceCode(method.getOriginalSourceFragment().getSourceCode());
                location = SpoonUtils.ConvertPosition2Location(methodNode, method.getOriginalSourceFragment().getSourcePosition());
                methodNode.setMethodLocation(location);
                SourceNode sourceNode = new SourceNode();
                sourceNode.setMethodNode(methodNode);
                DbUtils.UpdateSourceRuleNode(extractRuleObject, methodNode);
                return;
            }

            DbUtils.UpdateInvocationRuleNode(extractRuleObject, location);
        }
    }
}