package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileUtils {
    public static String csv = Paths.get("../csv").toAbsolutePath().toString();
    public static String collections = Paths.get(csv, "collections").toString();
    public static String sinks = Paths.get(csv, "sinks.csv").toString();
    public static String sources = Paths.get(csv, "sources.csv").toString();

    public static String Expanduser(String path) {
        String user=System.getProperty("user.home");
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
}
