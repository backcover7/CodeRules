package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.MarkdownUtils;

public class run {
    public static void main(String[] args) throws Exception {
        DbUtils dbUtils = new DbUtils();
        System.out.println("[*] Initialize rules ...");
        dbUtils.init();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

        String codebase = "src/main/test-cases/java/test.java";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, SpoonConfig.CommonLauncherFlag);

        MarkdownUtils markdownUtils = new MarkdownUtils();
        markdownUtils.init();

        Scanner scanner = new Scanner();
        System.out.println("[*] Analyzing the target source code ...");
        scanner.Collect();

        TaintedFlow taintedFlow = new TaintedFlow();
        System.out.println("[*] Processing global tainted flow analysis ...");
        taintedFlow.Scan();

        System.out.println("[*] Creating final scan reports ...");
        scanner.FlagThreats();

        markdownUtils.finish();
        DbUtils.conn.close();
        System.out.println("[+] Done!");
    }
}
