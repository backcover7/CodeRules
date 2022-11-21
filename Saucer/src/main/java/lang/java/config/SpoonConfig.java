package lang.java.config;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import utils.CharUtils;
import utils.FileUtils;

import java.util.ArrayList;

public class SpoonConfig {
    public Launcher getSpoonLauncher(String codebase) {
        ArrayList<String> jars = FileUtils.getExtensionFiles(ClassPath.SRC_MAIN_LIB, CharUtils.JarExtension, true);

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setSourceClasspath(jars.toArray(new String[]{}));
        launcher.addInputResource(codebase);
        return launcher;
    }

    //    https://spoon.gforge.inria.fr/launcher.html
    public MavenLauncher getSpoonMavenLauncher(String codebase) {
        MavenLauncher launcher = new MavenLauncher(codebase, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        launcher.addInputResource(codebase);
        return launcher;
    }

    public CtModel getSpoonModel(String codebase) {
        Launcher launcher = getSpoonLauncher(codebase);
        launcher.buildModel();
        return launcher.getModel();
    }

    public CtModel getSpoonMavenModel(String codebase) {
        MavenLauncher launcher = getSpoonMavenLauncher(codebase);
        launcher.buildModel();
        return launcher.getModel();
    }
}
