package csv.resolver.taint;

import utils.CsvUtils;
import utils.StringsUtils;
import org.apache.commons.text.StringSubstitutor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SemCSV {
    public final static String Namespace = "namespace";
    public final static String Type = "type";
    public final static String Name = "name";
    public final static String Kind = "kind";
    public final static String Category = "category";

    public final static String JDKPackage = "JDK";
    public final static String TPPPackage = "3PP";

    public final static String Constructor = "constructor";
    public final static String STATIC = "static";
    public final static String INSTANCE = "instance";
    public final static String STATICAndINSTANCE = "staticAndInstance";

    public final static String RuleKey = "rule";
    public final static String EitherPatternsKey = "EitherPatterns";

    private final static String PatternEitherTemplateWithKind = "      - pattern-either:        # ${kind}\n${EitherPatterns}";
    private final static String PatternEitherTemplateWithoutKind = "      - pattern-either:\n${EitherPatterns}";
    private final static String SpacePrefixPatternEitherTemplate = "  ";
    private final static String PatternTemplateWithKind = "      - pattern: ${rule}        # ${kind}";
    private final static String PatternTemplateWithoutKind = "      - pattern: ${rule}";
    private final static String StaticCallTemplate = "${namespace}.${type}.${name}(...)";
    private final static String InstanceCallTemplate = "(${namespace}.${type} $A).${name}(...)";
    private final static String ConstructorCallTemplate = "new ${namespace}.${type}(...)";

    public String getCallTypeColumn(HashMap<String, String> CSVMap) {
        if (CSVMap.get(Type).equals(CSVMap.get(Name)) || CSVMap.get(Name).equals("<init>")) {
            return Constructor;
        }

        String FullMethod = new StringJoiner(StringsUtils.dot).
                add(CSVMap.get(Namespace)).add(CSVMap.get(Type)).add(CSVMap.get(Name)).toString();

        if (getHardcodedList().containsKey(FullMethod)) {
            return getHardcodedList().get(FullMethod);
        }

        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(
                    new StringJoiner(StringsUtils.dot).
                        add(CSVMap.get(Namespace)).add(CSVMap.get(Type)).toString());
            String methodName = CSVMap.get(Name);
            Method[] methods = clazz.getDeclaredMethods();

            boolean StaticFlag = false;  // TODO: What if the method does not exist?
            HashSet<Boolean> flags = new HashSet<>();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    int modifier = method.getModifiers();
                    StaticFlag = Modifier.isStatic(modifier);
                    flags.add(StaticFlag);
                }
            }

            if (flags.size() != 1) {
                System.out.println("[!] Different modifiers in this method: " + FullMethod);
                Scanner scanner = new Scanner(System.in);
                System.out.println("[?] Please specify is this special method static call? Y/N/A >");
                String yesOrNo = scanner.nextLine();
                if (yesOrNo.equalsIgnoreCase("n")) {
                    StaticFlag = false;
                }
            }

            if (StaticFlag) {
                return STATIC;
            } else {
                return INSTANCE;
            }
        } catch (Exception e) {
            System.out.println("[!] The method " + FullMethod + " does not exist in classpath. Please reimport the regarding class into maven and rerun again.");
            return StringsUtils.empty;
        }
    }

    public String SemorlizeCSV(String CsvType, String CsvLine, HashSet<String> MethodSet) throws Exception {
        switch (CsvType) {
            case CodeQLCSV.CsvType:
                return new CodeQLCSV().SemorlizeCSV(CsvLine, MethodSet);
            case FindsecbugsCSV.CsvType:
                return new FindsecbugsCSV().SemorlizeCSV(CsvLine, MethodSet);
            default:
                return StringsUtils.empty;
        }
    }

    public static String getUniqueMethod(HashMap<String, String> CSVMap) {
        return new StringJoiner(StringsUtils.dot)
                .add(CSVMap.get(Namespace))
                .add(CSVMap.get(Type))
                .add(CSVMap.get(Name)).toString();
    }

    // CodeQL CSV and Findsecbugs CSV have lots of duplicate sink nodes based on semgrep rule syntax.
    // The duplicate items only differ in injected indexes so they can be merged with (...) in semgrep syntax.
    public static boolean IsPorcessedBefore(HashMap<String, String> CSVMap, HashSet<String> MethodSet) {
        return MethodSet.contains(getUniqueMethod(CSVMap));
    }

    String getPattern(HashMap<String, String> CSVMap, String calltype, HashSet<String> MethodSet) throws Exception {
        if (IsPorcessedBefore(CSVMap, MethodSet)) {
            return StringsUtils.empty;
        }
        CsvUtils.getCustomTaintCsvFromCSVMap(CSVMap, calltype, MethodSet);  // Backup to custom csv

        MethodSet.add(getUniqueMethod(CSVMap));        // Add to set structure for removing duplicates

        SeekVariants seekVariants = new SeekVariants();
        String patternEitherBlock = seekVariants.PatternEitherWithVariants(
                CSVMap.get(Namespace),
                CSVMap.get(Type),
                CSVMap.get(Name),
                CSVMap.get(Kind)
        );
        if (patternEitherBlock == null) {
            return WrapPatternListYaml(CSVMap, AssemblePatternElem(CSVMap, calltype));
        }
        return patternEitherBlock;
    }

    public String WrapPatternEitherBlockYaml(HashMap<String, String> CSVMap, ArrayList<String> patternList) {
        StringJoiner patternJoiner = new StringJoiner(StringsUtils.LF);
        for (String pattern : patternList) {
            patternJoiner.add(SpacePrefixPatternEitherTemplate + WrapPatternInnerEitherYaml(pattern));
        }

        HashMap<String, String> patternEitherMap = new HashMap<>();
        patternEitherMap.put(EitherPatternsKey, patternJoiner.toString());

        String patternEither;
        if (!CSVMap.get(Kind).equals(StringsUtils.empty)) {
            patternEitherMap.put(Kind, CSVMap.get(Kind));
            StringSubstitutor patternEitherWithKind = new StringSubstitutor(patternEitherMap);
            patternEither = patternEitherWithKind.replace(PatternEitherTemplateWithKind);
        } else {
            StringSubstitutor patternEitherWithoutKind = new StringSubstitutor(patternEitherMap);
            patternEither = patternEitherWithoutKind.replace(PatternEitherTemplateWithoutKind);
        }

        return patternEither;
    }

    String WrapPatternInnerEitherYaml(String pattern) {
        HashMap<String, String> patternMap = new HashMap<>();
        patternMap.put(RuleKey, pattern);

        String WrappedPattern;
        StringSubstitutor patternTemplateWithoutKind = new StringSubstitutor(patternMap);
        WrappedPattern = patternTemplateWithoutKind.replace(PatternTemplateWithoutKind);
        return WrappedPattern;
    }

    String WrapPatternListYaml(HashMap<String, String> CSVMap, ArrayList<String> patternList) {
        StringJoiner finalPattern = new StringJoiner(StringsUtils.LF);
        for (String pattern : patternList) {
            if (pattern.equals(StringsUtils.empty)) {
                continue;
            }
            HashMap<String, String> patternMap = new HashMap<>();
            patternMap.put(RuleKey, pattern);

            if (!CSVMap.get(Kind).equals(StringsUtils.empty)) {
                patternMap.put(Kind, CSVMap.get(Kind));
                StringSubstitutor patternTemplateWithKind = new StringSubstitutor(patternMap);
                pattern = patternTemplateWithKind.replace(PatternTemplateWithKind);
            } else {
                StringSubstitutor patternTemplateWithoutKind = new StringSubstitutor(patternMap);
                pattern = patternTemplateWithoutKind.replace(PatternTemplateWithoutKind);
            }

            finalPattern.add(pattern);

        }

        return finalPattern.toString();
    }

    ArrayList<String> AssemblePatternElem(HashMap<String, String> CSVMap, String calltype) {
        // Process InnerClass
        if (!CSVMap.get(SemCSV.Type).endsWith(StringsUtils.dollar)) {
            CSVMap.replace(SemCSV.Type,
                    CSVMap.get(SemCSV.Type).replaceAll(StringsUtils.dollarRegex, StringsUtils.dot));
        }

//        TODO: Waiting for the fix to https://github.com/returntocorp/semgrep/issues/6493
//        CSVMap.replace(SemCSV.Name,
//                CSVMap.get(SemCSV.Name).replaceAll(StringsUtils.dollarRegex, StringsUtils.dollarRegex));

        StringSubstitutor csvTemplate = new StringSubstitutor(CSVMap);
        ArrayList<String> patternList = new ArrayList<>();

        switch (calltype) {
            case Constructor:
                patternList.add(csvTemplate.replace(ConstructorCallTemplate));
                break;
            case STATIC:
                patternList.add(csvTemplate.replace(StaticCallTemplate));
                break;
            case INSTANCE:
                patternList.add(csvTemplate.replace(InstanceCallTemplate));
                break;
            case STATICAndINSTANCE:
                patternList.add(csvTemplate.replace(StaticCallTemplate));
                patternList.add(csvTemplate.replace(InstanceCallTemplate));
                break;
            default:
                break;
        }
        return patternList;
    }

    public static HashMap<String, String> getHardcodedList() {
        HashMap<String, String> HardcodedList = new HashMap<>();

        HardcodedList.put("java.util.regex.Pattern.compile", STATIC);
        HardcodedList.put("org.jdbi.v3.core.Jdbi.open", STATIC);
        HardcodedList.put("org.mvel2.templates.TemplateRuntime.execute", STATICAndINSTANCE);
        HardcodedList.put("org.hibernate.QueryProducer.createNativeQuery", INSTANCE);
        HardcodedList.put("org.hibernate.QueryProducer.createQuery", INSTANCE);
        HardcodedList.put("org.hibernate.QueryProducer.createSQLQuery", INSTANCE);
        HardcodedList.put("javax.ws.rs.core.ResponseBuilder.header", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getBody", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getContentLength", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getCookies", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getHeaders", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getPath", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getQuery", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getQueryParams", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getRawUri", INSTANCE);
        HardcodedList.put("ratpack.http.Request.getUri", INSTANCE);
        HardcodedList.put("ratpack.http.Request.oneCookie", INSTANCE);
        HardcodedList.put("org.apache.commons.ognl.Node.getValue", INSTANCE);
        HardcodedList.put("org.apache.commons.ognl.Node.setValue", INSTANCE);
        HardcodedList.put("org.apache.commons.ognl.Ognl.getValue", STATIC);
        HardcodedList.put("org.apache.commons.ognl.Ognl.setValue", STATIC);
        HardcodedList.put("org.springframework.ldap.LdapOperations.findByDn", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.list", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.listBindings", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.lookup", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.lookupContext", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.rename", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.search", INSTANCE);
        HardcodedList.put("org.springframework.ldap.LdapOperations.searchForObject", INSTANCE);
        HardcodedList.put("org.apache.commons.ognl.enhance.ExpressionAccessor.get", INSTANCE);
        HardcodedList.put("org.apache.commons.ognl.enhance.ExpressionAccessor.set", INSTANCE);
        HardcodedList.put("androidx.slice.SliceProvider.onBindSlice", INSTANCE);
        HardcodedList.put("androidx.slice.SliceProvider.onCreatePermissionRequest", INSTANCE);
        HardcodedList.put("ratpack.handling.Context.parse", INSTANCE);
        HardcodedList.put("org.apache.commons.io.input.Tailer$Tailable.getRandomAccess", INSTANCE);
        HardcodedList.put("org.apache.commons.io.RandomAccessFileMode.create", INSTANCE);   // ?
        HardcodedList.put("android.content.ContentInterface.call", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.delete", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.getType", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.insert", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.openAssetFile", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.openFile", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.openTypedAssetFile", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.query", INSTANCE);
        HardcodedList.put("android.content.ContentInterface.update", INSTANCE);
        HardcodedList.put("androidx.slice.SliceProvider.onMapIntentToUri", INSTANCE);
        HardcodedList.put("androidx.slice.SliceProvider.onSlicePinned", INSTANCE);
        HardcodedList.put("androidx.slice.SliceProvider.onSliceUnpinned", INSTANCE);
        HardcodedList.put("androidx.core.app.AlarmManagerCompat.setAlarmClock", STATIC);
        HardcodedList.put("androidx.core.app.AlarmManagerCompat.setAndAllowWhileIdle", STATIC);
        HardcodedList.put("androidx.core.app.AlarmManagerCompat.setExact", STATIC);
        HardcodedList.put("androidx.core.app.AlarmManagerCompat.setExactAndAllowWhileIdle", STATIC);
        HardcodedList.put("androidx.core.app.NotificationManagerCompat.notify", STATIC);
        HardcodedList.put("org.apache.commons.jexl2.JexlExpression.callable", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl2.JexlExpression.evaluate", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl2.JexlScript.callable", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl2.JexlScript.execute", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl3.Expression.callable", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl3.Expression.evaluate", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl3.Script.callable", INSTANCE);
        HardcodedList.put("org.apache.commons.jexl3.Script.execute", INSTANCE);
        HardcodedList.put("org.owasp.encoder.tag.ForHtmlTag.doTag", INSTANCE);
        HardcodedList.put("org.apache.turbine.om.peer.BasePeer.executeQuery", STATIC);
        HardcodedList.put("java.net.http.WebSocket$Listener.onText", INSTANCE);

        return HardcodedList;
    }
}
