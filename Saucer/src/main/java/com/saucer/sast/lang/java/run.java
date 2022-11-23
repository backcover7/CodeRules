package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import spoon.reflect.CtModel;
import com.saucer.sast.utils.DbUtils;

public class run {
    public static void main(String[] args) throws Exception {
        DbUtils dbUtils = new DbUtils();
        dbUtils.init();

        SpoonConfig spoonConfig = new SpoonConfig();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

        String codebase = "src/main/test-cases/java/";
        CtModel model = spoonConfig.getSpoonModel(codebase);

        Scanner scanner = new Scanner();
        scanner.Scan(model);

        DbUtils.conn.close();
    }
}
