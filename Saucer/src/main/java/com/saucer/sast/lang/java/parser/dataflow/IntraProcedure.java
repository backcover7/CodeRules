package com.saucer.sast.lang.java.parser.dataflow;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.utils.FileUtils;
import com.saucer.sast.utils.SemgrepUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IntraProcedure {
    private final static String taint2args = "rules:\n" +
            "  - id: taint2args\n" +
            "    options:\n" +
            "      symbolic_propagation: true\n" +
            "      taint_unify_mvars: true\n" +
            "    mode: taint\n" +
            "    pattern-sources:\n" +
            "      - patterns:\n" +
            "          - pattern-inside: ${signature} {$...BODY;}\n" +
            "          - pattern-inside: $BODY\n" +
            "          - pattern: |\n" +
            "              <... $VAR${SourceIndex} ...>\n" +
            "    pattern-sinks:\n" +
            "      - pattern: $VAR${TargetIndex}\n" +
            "    paths:\n" +
            "      include:\n" +
            "        - ${position}\n" +
            "    message: Semgrep found a taint flow from specific argument to other arguments\n" +
            "    languages: [java]\n" +
            "    severity: WARNING";
    private final static String SOURCEINDEX = "SourceIndex";
    private final static String TARGETINDEX = "TargetIndex";

    private final static String taint2return = "rules:\n" +
            "  - id: taint2return\n" +
            "    options:\n" +
            "      symbolic_propagation: true\n" +
            "    mode: taint\n" +
            "    pattern-sources:\n" +
            "      - patterns:\n" +
            "          - pattern-inside: ${signature} {...}\n" +
            "          - pattern: $VAR\n" +
            "    pattern-sinks:\n" +
            "      - pattern: return ...;\n" +
            "    paths:\n" +
            "      include:\n" +
            "        - ${position}\n" +
            "    message: Semgrep found a taint flow from argument to return value\n" +
            "    languages: [java]\n" +
            "    severity: WARNING";
    private final static String SIGNATURE = "signature";
    private final static String METHODPATH = "methodpath";

//    public ArrayList<Integer> MethodTaintAnalysis(CtMethod<?> method) {
//        method.
//    }

    public boolean IsReturnTainted(String signature, String methodPath) {
        Map<String, String> ruleMap = new HashMap<>();
        ruleMap.put(SIGNATURE, signature);
        ruleMap.put(METHODPATH, methodPath);

        StringSubstitutor stringSubstitutor = new StringSubstitutor(ruleMap);
        String rule = stringSubstitutor.replace(taint2return);

        try {
            File file = File.createTempFile("taint2return.yaml", null);
            FileUtils.WriteFile(file.getAbsolutePath(), rule, false);
            file.deleteOnExit();

            if (SemgrepUtils.RunSemgrepRule(file.getAbsolutePath(), SpoonConfig.codebase).size() != 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<Integer> IndexTaintedByArg(int SourceIndex, int size, String signature, String methodPath) {
        ArrayList<Integer> TaintedIndex = new ArrayList<>();

        Map<String, String> ruleMap = new HashMap<>();
        ruleMap.put(SOURCEINDEX, String.valueOf(SourceIndex));
        ruleMap.put(SIGNATURE, signature);
        ruleMap.put(METHODPATH, methodPath);

        try {
            for (int i = 0; i < size; i++) {
                if (i == SourceIndex) {
                    continue;
                }
                ruleMap.put(TARGETINDEX, String.valueOf(i));
                StringSubstitutor stringSubstitutor = new StringSubstitutor(ruleMap);
                String rule = stringSubstitutor.replace(taint2return);

                File file = File.createTempFile("taint2args.yaml", null);
                FileUtils.WriteFile(file.getAbsolutePath(), rule, false);
                try {
                    if (SemgrepUtils.RunSemgrepRule(file.getAbsolutePath(), SpoonConfig.codebase).size() != 0) {
                        TaintedIndex.add(i);
                    }
                } finally {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TaintedIndex;
    }
}