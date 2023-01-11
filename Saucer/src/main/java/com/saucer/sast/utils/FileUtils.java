package com.saucer.sast.utils;

import com.saucer.sast.Main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileUtils {
    public final static String OutputDirectory = Paths.get(Main.output).toAbsolutePath().normalize().toString();

    public static String readResourceFile2String(String filename) {
        InputStream resourceFile = FileUtils.class.getResourceAsStream("/" + filename);
        return ReadFile2String(resourceFile);
    }

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

    public static String ReadFile2String(InputStream in) {
        String content = null;
        try {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
