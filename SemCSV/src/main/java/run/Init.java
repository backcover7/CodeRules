package run;

import utils.ClasspathUtils;

import java.io.File;

public class Init {
    public static void main(String[] args) throws Exception {
        File[] directories = new File[]{
                new File(ClasspathUtils.JDK),
                new File(ClasspathUtils.Dependencies)
        };
        for (File file : directories) {
            file.mkdir();
        }

        ClasspathUtils.DownloadJars(true);
    }
}
