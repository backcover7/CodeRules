package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.PropertyConfig;
import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

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

    @Option(names = {"-o", "--output"}, defaultValue = ".", description = "The path of output report.\n* Default is current directory.")
    private static String output;

    public static Properties properties;

    public void run() {
        try {
            PropertyConfig propertyConfig = new PropertyConfig();
            properties = propertyConfig.getProperties();
            properties.setProperty(PropertyConfig.RULES, rules);
            properties.setProperty(PropertyConfig.DEPENDENCY, dependency);
            properties.setProperty(PropertyConfig.OUTPUT, output);

            System.out.println();
            System.out.println(CharUtils.banner);
            try {
                Runtime.getRuntime().exec("semgrep");
            } catch (Exception e) {
                System.err.println("[!] Error: Please install Semgrep CLI! (https://semgrep.dev/docs/getting-started/)");
                System.exit(1);
            }

            System.out.println("[*] Initialize rules ...");
            DbUtils.init();
            DbUtils.connect();

            SpoonConfig spoonConfig = new SpoonConfig();
            // todo
//            codebase = "/Users/kang.hou/Documents/CodeRules/test-cases/java/test.java";
//            codebase = "/Users/kang.hou/Documents/CodeRules/test-cases/java/JdbcRowSetImpl.java";
            codebase = "/Users/kang.hou/Downloads/commons-beanutils-master/";
            maven = true;
            if (!maven) {
                spoonConfig.init(codebase, dependency);
            } else {
                spoonConfig.init(codebase);
            }

            Scanner scanner = new Scanner();
            scanner.Scan();

            DbUtils.conn.close();
            System.out.println("[!] Done!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}