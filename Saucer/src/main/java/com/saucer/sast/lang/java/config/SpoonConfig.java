package com.saucer.sast.lang.java.config;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonException;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.FileUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

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

    private Launcher getSpoonLauncher(String jarlibPath) {
        // TODO add .m2
        File directory = new File(Paths.get(FileUtils.Expanduser(jarlibPath)).toAbsolutePath().toString());
        IOFileFilter jarFilter = new SuffixFileFilter(CharUtils.JarExtension);
        Collection<File> jars = org.apache.commons.io.FileUtils.listFiles(
                directory, jarFilter, TrueFileFilter.INSTANCE);

        ArrayList<String> jarArrays = new ArrayList<>();
        jars.parallelStream().forEach(jar -> {
            jarArrays.add(jar.getAbsolutePath());
        });

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setSourceClasspath(jarArrays.toArray(new String[0]));
        launcher.addInputResource(codebase);
        return launcher;
    }

    //    https://spoon.gforge.inria.fr/launcher.html
    private MavenLauncher getSpoonMavenLauncher() {
        MavenLauncher launcher = null;
        try {
            launcher = new MavenLauncher(codebase, MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        } catch (SpoonException e) {
            if (e.getMessage().contains("Unable to read the pom")) {
                System.err.println("[!] Error: The target project does not have pom.xml!");
                System.exit(1);
            }
        }
        assert launcher != null;
        launcher.addInputResource(codebase);
        return launcher;
    }

    private void setEnv(Environment env) {
        env.disableConsistencyChecks();
        env.setCommentEnabled(false);
    }
}
