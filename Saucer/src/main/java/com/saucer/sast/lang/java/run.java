package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class run {
    public static void main(String[] args) throws Exception {
        FileUtils.cleanDirectory(new File(com.saucer.sast.utils.FileUtils.tmp));
        System.out.println();
        System.out.println(CharUtils.banner);
        try {
            Runtime.getRuntime().exec("semgrep");
        } catch (Exception e) {
            System.out.println("[!] Please install Semgrep CLI! (https://semgrep.dev/docs/getting-started/)");
            System.exit(0);
        }

        DbUtils dbUtils = new DbUtils();
        System.out.println("[*] Initialize rules ...");
        dbUtils.init();

//        String codebase = "/Users/kang.hou/Documents/CodeRules/Saucer";
        String codebase = "src/main/test-cases/java/test.java";
//        String codebase = "/Users/kang.hou/Documents/CodeRules/Saucer/src/main/test-cases/java/JdbcRowSetImpl.java";
//        String codebase = "~/Downloads/org";
//        String codebase = "/Users/kang.hou/Desktop/click-nodeps-2.3.0/";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, "");

        Scanner scanner = new Scanner();
        scanner.Scan();

        DbUtils.conn.close();
    }
}
