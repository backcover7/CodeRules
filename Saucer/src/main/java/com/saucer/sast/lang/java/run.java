package com.saucer.sast.lang.java;

import com.saucer.sast.lang.java.config.SpoonConfig;

import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.DbUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class run {
    public static void main(String[] args) throws Exception {
        // TODO
        System.out.println();
        String banner = " ________  ________  ___  ___  ________  _______   ________          ___       \n" +
                "|\\   ____\\|\\   __  \\|\\  \\|\\  \\|\\   ____\\|\\  ___ \\ |\\   __  \\        |\\  \\      \n" +
                "\\ \\  \\___|\\ \\  \\|\\  \\ \\  \\\\\\  \\ \\  \\___|\\ \\   __/|\\ \\  \\|\\  \\       \\ \\  \\     \n" +
                " \\ \\_____  \\ \\   __  \\ \\  \\\\\\  \\ \\  \\    \\ \\  \\_|/_\\ \\   _  _\\       \\ \\  \\    \n" +
                "  \\|____|\\  \\ \\  \\ \\  \\ \\  \\\\\\  \\ \\  \\____\\ \\  \\_|\\ \\ \\  \\\\  \\|       \\ \\__\\   \n" +
                "    ____\\_\\  \\ \\__\\ \\__\\ \\_______\\ \\_______\\ \\_______\\ \\__\\\\ _\\        \\|__|   \n" +
                "   |\\_________\\|__|\\|__|\\|_______|\\|_______|\\|_______|\\|__|\\|__|           ___ \n" +
                "   \\|_________|                                                           |\\__\\\n" +
                "                                                                          \\|__|\n" +
                "                                                                               ";
        System.out.println(banner);
        try {
            Runtime.getRuntime().exec("semgrep");
        } catch (Exception e) {
            System.out.println("[!] Please install Semgrep CLI! (https://semgrep.dev/docs/getting-started/)");
            System.exit(0);
        }

        FileUtils.cleanDirectory(new File(com.saucer.sast.utils.FileUtils.tmp));

        DbUtils dbUtils = new DbUtils();
        System.out.println("[*] Initialize rules ...");
        dbUtils.init();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

//        String codebase = "/Users/kang.hou/Documents/CodeRules/Saucer";
//        String codebase = "src/main/test-cases/java/test.java";
//        String codebase = "/Users/kang.hou/Documents/CodeRules/Saucer/src/main/test-cases/java/JdbcRowSetImpl.java";
//        String codebase = "~/Downloads/org";
        String codebase = "/Users/kang.hou/Desktop/click-nodeps-2.3.0/";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, "");

        Scanner scanner = new Scanner();
        scanner.Scan();

        DbUtils.conn.close();
    }
}
