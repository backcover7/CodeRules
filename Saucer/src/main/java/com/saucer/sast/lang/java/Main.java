package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.PropertyConfig;
import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.Properties;

//@Command(name = "Java", mixinStandardHelpOptions = true, version = "Saucer/0.1",
//        description = "Scan Java codebase to find security threats.")
public class Main implements Runnable {
    @Parameters(index = "0", description = "The path of target codebase.")
    private static String codebase;

    @Option(names = {"-r", "--rules"}, defaultValue = "../../csv/nodes", description = "The path of security analysis rules.\n* Default is ../csv/nodes.")
    private static String rules;

    @Option(names = {"-m", "--maven"}, description = "Specify if the target is built by Maven")
    private static boolean maven;

    @Option(names = {"-d", "--dependency"}, defaultValue = "", description = "The path of dependency jar files of target codebase.\n* Default is None.")
    private static String dependency;

    @Option(names = {"-f", "--flow"}, defaultValue = "all", completionCandidates = TaintedFlowFlagsCandidate.class,
            description = "Designate the specific flow you would like to analyze.\nCandidate flows: [all], [web], [gadget], [json].\n* Default is [all].\n[WARNING] The JSON flow might cost much time!")
    private static String flow;

    @Option(names = {"-o", "--output"}, defaultValue = ".", description = "The path of output report.\n* Default is current directory.")
    private static String output;

    static class TaintedFlowFlagsCandidate extends ArrayList<String> {
        TaintedFlowFlagsCandidate() {
            super(TaintedFlow.TaintedFlowFlags);
        }
    }

    public static Properties properties;

    public void run() {
        try {
            PropertyConfig propertyConfig = new PropertyConfig();
            properties = propertyConfig.getProperties();
            properties.setProperty(PropertyConfig.RULES, rules);
            properties.setProperty(PropertyConfig.DEPENDENCY, dependency);
            properties.setProperty(PropertyConfig.FLOW, flow);
            properties.setProperty(PropertyConfig.OUTPUT, output);

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