package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import spoon.Launcher;
import spoon.reflect.CtModel;
import com.saucer.sast.utils.DbUtils;

public class run {
    public static void main(String[] args) throws Exception {
        DbUtils dbUtils = new DbUtils();
        dbUtils.init();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

        String codebase = "src/main/test-cases/java/sink1.java";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, SpoonConfig.CommonLauncherFlag);

        Scanner scanner = new Scanner();
        scanner.Scan();

        TaintedFlow taintedFlow = new TaintedFlow();
        taintedFlow.StartFromSource();
        DbUtils.conn.close();
    }
}
