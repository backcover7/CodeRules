package com.saucer.sast.lang.javascript;

import com.saucer.sast.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Paths;

public class run {

    private static void start(String codebase) throws IOException, InterruptedException {
        DependencyConfusion dependencyConfusion = new DependencyConfusion();
        dependencyConfusion.Scan(codebase);
    }

    public static void main(String[] args) throws Exception {
        String codebase = Paths.get(FileUtils.Expanduser(
                "~/Downloads/sfdc/"
        )).toAbsolutePath().normalize().toString();
        start(codebase);
    }
}
