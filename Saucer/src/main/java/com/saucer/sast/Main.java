package com.saucer.sast;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

//@Command(name = "Java", mixinStandardHelpOptions = true, version = "Saucer/0.1",
//        description = "Scan Java codebase to find security threats.")
public class Main implements Runnable {
    @Parameters(index = "0", description = "The path of target codebase.")
    public static String codebase;

    @Option(names = {"-c", "--csv"}, defaultValue = "../csv/nodes", description = "The path of security analysis csv rules.")
    public static String csv;

    @Option(names = {"-r", "--rule"}, defaultValue = "../rules", description = "The path of security analysis filter rules.")
    public static String rule;

    @Option(names = {"-m", "--maven"}, description = "Specify if the target is built by Maven")
    public static boolean maven;

    @Option(names = {"-d", "--dependency"}, defaultValue = "", description = "The path of dependency jar files of target codebase.\n* Default is None.")
    public static String dependency;

    @Option(names = {"-o", "--output"}, defaultValue = ".", description = "The path of output report.\n* Default is current directory.")
    public static String output;

    public void run() {
        try {
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