package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.DbUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class run {
    public static void main(String[] args) throws Exception {
        FileUtils.cleanDirectory(new File(com.saucer.sast.utils.FileUtils.tmp));

        DbUtils dbUtils = new DbUtils();
        System.out.println("[*] Initialize rules ...");
        dbUtils.init();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

//        String codebase = "/Users/kang.hou/Documents/CodeRules/Saucer";
        String codebase = "src/main/test-cases/java/JdbcRowSetImpl.java";
//        String codebase = "src/main/test-cases/java/test.java";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, SpoonConfig.CommonLauncherFlag);

        Scanner scanner = new Scanner();
        scanner.Scan();

        DbUtils.conn.close();
    }
}
