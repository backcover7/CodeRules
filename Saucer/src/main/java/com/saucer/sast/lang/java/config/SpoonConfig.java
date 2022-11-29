package com.saucer.sast.lang.java.config;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.FileUtils;

import java.nio.file.Paths;
import java.util.ArrayList;

public class SpoonConfig {
    public final static String CommonLauncherFlag = "COMMON";
    public final static String MavenLauncherFlag = "MAVEN";

    public static String codebase;
    public static Launcher launcher;
    public static CtModel model;

    public void init(String codebase, String LauncherFlag) throws Exception {
        SpoonConfig.codebase = Paths.get(codebase).toAbsolutePath().toString();
        switch (LauncherFlag) {
            case CommonLauncherFlag:
                launcher = getSpoonLauncher();
                break;
            case MavenLauncherFlag:
                launcher = getSpoonMavenLauncher();
                break;
            default:
                break;
        }
        if (launcher != null) {
            launcher.buildModel();
            model = launcher.getModel();
        } else {
            throw new Exception("[!] Failed in getting launcher from codebase");
        }
    }

    private Launcher getSpoonLauncher() {
        ArrayList<String> jars = FileUtils.getExtensionFiles(ClassPath.SRC_MAIN_LIB, CharUtils.JarExtension, true);

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setSourceClasspath(jars.toArray(new String[]{}));
        launcher.addInputResource(codebase);
        return launcher;
    }

    //    https://spoon.gforge.inria.fr/launcher.html
    private MavenLauncher getSpoonMavenLauncher() {
        MavenLauncher launcher = new MavenLauncher(codebase, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        launcher.addInputResource(codebase);
        return launcher;
    }
}
