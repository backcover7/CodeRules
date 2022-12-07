package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.FileUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.Properties;

@Command(name = "Java", mixinStandardHelpOptions = true, version = "Saucer/0.1",
        description = "Scan Java codebase to find security threats.")
public class Main implements Runnable {
    public static Properties props = new Properties();

    @Parameters(index = "0", description = "The path of target codebase.")
    private static String codebase;

    @Option(names = {"-r", "--rules"}, defaultValue = "../csv/nodes", description = "The path of security analysis rules.")
    private static String rules;

    @Option(names = {"-m", "--maven"}, description = "Specify if the target is built by Maven")
    private static boolean maven;

    @Option(names = {"-d", "--dependency"}, defaultValue = "", description = "The path of dependency jar files of target codebase.")
    private static String dependency;

    @Option(names = {"-f", "--flow"}, defaultValue = "all", completionCandidates = TaintedFlowFlagsCandidate.class,
            description = "Designate the specific flow you would like to analyze. [WARNING] The JSON flow might cost much time!")
    private static String flow;

    @Option(names = {"-o", "--output"}, defaultValue = "../", description = "The path of output report.")
    private static String output;

    static class TaintedFlowFlagsCandidate extends ArrayList<String> {
        TaintedFlowFlagsCandidate() {
            super(TaintedFlow.TaintedFlowFlags);
        }
    }

    public void run() {
        try {
            props.setProperty("rules", rules);
            props.setProperty("dependency", dependency);
            props.setProperty("flow", flow);
            props.setProperty("output", output);

            System.out.println();
            System.out.println(CharUtils.banner);
            try {
                Runtime.getRuntime().exec("semgrep");
            } catch (Exception e) {
                System.err.println("[!] Error: Please install Semgrep CLI! (https://semgrep.dev/docs/getting-started/)");
                System.exit(1);
            }

            DbUtils dbUtils = new DbUtils();
            System.out.println("[*] Initialize rules ...");
            dbUtils.init();

            SpoonConfig spoonConfig = new SpoonConfig();
            if (!maven) {
                spoonConfig.init(codebase, dependency);
            } else {
                spoonConfig.init(codebase);
            }

            Scanner scanner = new Scanner();
            scanner.Scan(flow);

            DbUtils.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}