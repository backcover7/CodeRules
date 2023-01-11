package com.saucer.sast.lang.java.parser.filter;

import com.contrastsecurity.sarif.Location;
import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.nodes.*;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.SpoonUtils;
import org.reflections.Reflections;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.Filter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class Extracter {
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
    private Extracter.ExtractRuleObject FilterElements(Class<? extends FilterHelper> filter) {
        try {
            Extracter.ExtractRuleObject extractRuleObject = LoadFilter(filter);
            extractRuleObject.setElems(SpoonConfig.model.getRootPackage().filterChildren(
                    extractRuleObject.getFilter()
//                    new TemplateFilter()
            ).list());
            return extractRuleObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Extracter.ExtractRuleObject LoadFilter(Class<? extends FilterHelper> filter) {
        try {
            Filter<?> filterObj = filter.getDeclaredConstructor().newInstance();
            RuleNode ruleNode = new RuleNode();
            Field[] fields = filter.getDeclaredFields();
            String RuleFlag = null;
            for (Field field : fields) {
                switch (field.getName()) {
                    case "category":
                        ruleNode.setCategory(field.get(filterObj).toString());
                        break;
                    case "kind":
                        ruleNode.setKind(field.get(filterObj).toString());
                        break;
                    case "rule":
                        ruleNode.setRule(field.get(filterObj).toString());
                        break;
                    default:
                        RuleFlag = field.getName();
                        break;
                }
            }
            ruleNode.setCategory(filter.getDeclaredField(RuleNode.CATEGORY).get(filterObj).toString());
            ruleNode.setKind(filter.getDeclaredField(RuleNode.KIND).get(filterObj).toString());
            ruleNode.setRule(filter.getDeclaredField(RuleNode.RULE).get(filterObj).toString());


            try {
                RuleNode.class.getDeclaredField(RuleFlag);
            } catch (Exception e) {
                System.err.println("[!] Please recheck the flag field in your rule.");
                System.exit(1);
            }

            Extracter.ExtractRuleObject extractRuleObject = new Extracter.ExtractRuleObject();
            extractRuleObject.setRuleNode(ruleNode);
            extractRuleObject.setFilter(filterObj);
            extractRuleObject.setRuleFlag(RuleFlag);
            return extractRuleObject;
        } catch (Exception e) {
            System.err.println("[!] Wrong format of Filter rule! Please check your rule again.");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public void FilterAndUpdate() {
        Reflections reflections = new Reflections("com.saucer.sast.lang.java.rule");
        Set<Class<? extends FilterHelper>> filters = reflections.getSubTypesOf(FilterHelper.class);
        for (Class<? extends FilterHelper> filter : filters) {
            FilterAndUpdate(filter);
        }
    }

    private void FilterAndUpdate(Class<? extends FilterHelper> filter) {
        Extracter.ExtractRuleObject extractRuleObject = FilterElements(filter);
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
                DbUtils.UpdateSourceRuleNode(extractRuleObject, methodNode);
                return;
            }

            DbUtils.UpdateInvocationRuleNode(extractRuleObject, location);
        }
    }
}