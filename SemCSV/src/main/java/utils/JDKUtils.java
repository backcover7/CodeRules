package utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

public class JDKUtils {
    private final static String JAVA_HOME = System.getProperty("java.home");
    private final static String JAVA_LIB = Paths.get(JAVA_HOME, "lib").toString();
    private final static String JAVA_EXT = Paths.get(JAVA_LIB, "ext").toString();

    public static void CopyJDKJars() throws Exception {
        ArrayList<String> jars = new ArrayList<>();
        jars.addAll(FilesUtils.getExtensionFiles(JAVA_LIB, StringsUtils.JarExtension, false));
        jars.addAll(FilesUtils.getExtensionFiles(JAVA_EXT, StringsUtils.JarExtension, false));

        for (String jar : jars) {
//            BLACKLIST = JDK_BLACKLIST_SWITCH;
            /**
             *    Class<?> fx  = Class.forName("com.sun.deploy.uitoolkit.impl.fx.FXApplet2Adapter");
             *    fx.getMethods();  // throws exception: Caused by: java.lang.ClassNotFoundException: com.sun.deploy.uitoolkit.Applet2Adapter
             *
             *    Class<?> applet = Class.forName("com.sun.deploy.uitoolkit.Applet2Adapter");
             *    applet.getMethods();  // Succeed!??
             */
//            if(BLACKLIST && Arrays.stream(Blacklist).anyMatch(jar::contains)) {
//                continue;
//            }
            FileUtils.copyFileToDirectory(new File(jar), new File(ClasspathUtils.JDK));
        }
    }
}
