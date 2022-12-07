package com.saucer.sast.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileUtils {
    public static String csv = Paths.get("../csv").toAbsolutePath().toString();
    public final static String NodesDirectory = Paths.get(FileUtils.csv, "nodes").toString();
    public static String collections = Paths.get(csv, "collections").toString();
    public static String sinks = Paths.get(collections, "sinks.csv").toString();
    public static String sources = Paths.get(collections, "sources.csv").toString();
    public static String resources = Paths.get("src/main/resources").toAbsolutePath().toString();

    public static String tmp = Paths.get(resources, "tmp").toString();
    public static String taint4source = Paths.get(resources, "templates/taint4source.yaml").toString();
    public static String taint2invocation = Paths.get(resources, "templates/taint2invocation.yaml").toString();
    public static String taint2nonparaminvocation = Paths.get(resources, "templates/taint2nonparaminvocation.yaml").toString();

    public static String report = Paths.get(resources, "report").toString();

    public static String Expanduser(String path) {
        String user = System.getProperty("user.home");
        return path.replaceFirst("~", user);
    }

    public static ArrayList<String> getExtensionFiles(String directory, String extension, boolean recursive) {
        directory = Expanduser(directory);
        ArrayList<String> jars = new ArrayList<>();
        try {
            for (File file : org.apache.commons.io.FileUtils.listFiles(
                    new File(directory),
                    new String[]{extension.replaceAll(CharUtils.dotRegex, CharUtils.empty)},
                    recursive)
            ) {
                jars.add(file.getAbsolutePath());
            }
        } catch (Exception e) {}
        return jars;
    }

    public static void WriteFileLn(String filePath, String data, boolean append) {
        WriteFile(filePath, data + CharUtils.LF, append);
    }

    public static void WriteFile(String filePath, String data, boolean append) {
        File file = new File(filePath);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), append);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String ReadFile2String(String filePath) {
        String content = null;
        try {
            content = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
