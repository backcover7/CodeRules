package com.saucer.sast.lang.java.config;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.FileUtils;

import java.nio.file.Paths;
import java.util.ArrayList;

public class SpoonConfig {
    public static String codebase;
    public static Launcher launcher;
    public static CtModel model;

    public void init(String codebase) {
        SpoonConfig.codebase = Paths.get(FileUtils.Expanduser(codebase)).toAbsolutePath().toString();
        launcher = getSpoonMavenLauncher();
        setEnv(launcher.getEnvironment());
        launcher.buildModel();
        model = launcher.getModel();
    }

    public void init(String codebase, String jarLibsPath) {
        SpoonConfig.codebase = Paths.get(FileUtils.Expanduser(codebase)).toAbsolutePath().toString();
        launcher = getSpoonLauncher(jarLibsPath);
        setEnv(launcher.getEnvironment());
        launcher.buildModel();
        model = launcher.getModel();
    }

    private Launcher getSpoonLauncher(String jarLibsPath) {
        ArrayList<String> jars = FileUtils.getExtensionFiles(
                Paths.get(FileUtils.Expanduser(jarLibsPath)).toAbsolutePath().toString(),
                CharUtils.JarExtension, true);

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

    private void setEnv(Environment env) {
        env.disableConsistencyChecks();
        env.setCommentEnabled(false);
    }
}
