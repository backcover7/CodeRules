package com.saucer.sast.lang.javascript;

import com.saucer.sast.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Paths;

public class run {
    private static String codebase;

    private static void start() throws IOException, InterruptedException {
        DependencyConfusion dependencyConfusion = new DependencyConfusion();
        dependencyConfusion.Scan(codebase);
    }

    public static void main(String[] args) throws Exception {
        codebase = Paths.get(FileUtils.Expanduser("~/Downloads/sfdc/raptor-ssr/packages/raptor-ssr/package.json")).toAbsolutePath().toString();
        start();
    }
}
