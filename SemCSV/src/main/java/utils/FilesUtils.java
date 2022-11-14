package utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FilesUtils {
    public final static String csv_folder = Paths.get("../csv").toAbsolutePath().normalize().toString();
    public final static String class_csv = Paths.get(csv_folder,"class_csv").toString();
    public final static String JDK_class_csv = Paths.get(class_csv, "JDK").toString();
    public final static String Other_class_csv = Paths.get(class_csv, "Others").toString();
    public final static String method_csv = Paths.get(csv_folder,"method_csv").toString();
    public final static String JDK_method_csv = Paths.get(method_csv, "JDK").toString();
    public final static String Other_method_csv = Paths.get(method_csv, "Others").toString();
    public final static String taint_csv = Paths.get(csv_folder,"taint_csv").toString();

    public final static String sinks_csv = Paths.get(taint_csv, "sinks.csv").toString();
    public final static String sources_csv = Paths.get(taint_csv, "sources.csv").toString();

    public final static String METAINF = "META-INF";

    public static List<String> getResourcesFromJar(String JarPath, String extension, boolean path) throws IOException {
        List<String> resources = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(Files.newInputStream(Paths.get(Expanduser(JarPath))));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(extension)) {
                String resource = entry.getName();
                if (extension.equals(StringsUtils.ClassExtension) &&
                        Paths.get(resource).toString().contains(FilesUtils.METAINF)){
                    continue;
                }
                if (!path) {
                    resource = resource.replaceAll(StringsUtils.slash, StringsUtils.dot);
                    resources.add(resource.substring(0, resource.length() - extension.length()));
                } else {
                    resources.add(resource);
                }
            }
        }
        return resources;
    }

    public static ArrayList<String> getExtensionFiles(String directory, String extension, boolean recursive) {
        directory = Expanduser(directory);
        ArrayList<String> jars = new ArrayList<>();
        try {
            for (File file : FileUtils.listFiles(
                    new File(directory),
                    new String[]{extension.replaceAll(StringsUtils.dotRegex, StringsUtils.empty)},
                    recursive)
            ) {
                jars.add(file.getAbsolutePath());
            }
        } catch (Exception e) {}
        return jars;
    }

    public static String Expanduser(String path) {
        String user=System.getProperty("user.home");
        return path.replaceFirst("~", user);
    }

    public static String getFolder(boolean JDK, boolean clazz) {
        // JDK flag     ->  true: CSV for JDK;      false: CSV for Others
        // clazz flag   ->  true: CSV for class;    false: CSV for method
        String folder;
        if(JDK && clazz) {
            folder = JDK_class_csv;
        } else if (JDK && !clazz) {
            folder = JDK_method_csv;
        } else if (!JDK && clazz) {
            folder = Other_class_csv;
        } else {   //  !JDK && !clazz
            folder = Other_method_csv;
        }
        return folder;
    }

    public static boolean FileExistingPrompt(String directory, String filename, String csvType) throws IOException {
        File file = new File(Paths.get(directory, Expanduser(filename)).toString());

        if (file.exists()) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("[?] The target file " + file.getName() + " exists. Do you still want to overwrite " + csvType + " into it? Y/N > ");
            String yesOrNo = scanner.nextLine();
            if (yesOrNo.equalsIgnoreCase("n")) {
                System.out.println("Skip writing " + csvType + " into " + file.getName());
                return false;
            }
            FileUtils.delete(file);
        }
        return true;
    }

    public static void PrintOrWriteMethodCSV(String CSV, boolean print, String filename, boolean JDK) throws Exception {
        if (print) {
            System.out.print(CSV);
        } else {
            WriteMethodCSVFile(filename, CSV, JDK);
        }
    }

    public static void PrintOrWriteClassCSV(String CSV, boolean print, String filename, boolean JDK) throws Exception {
        if (print) {
            System.out.print(CSV);
        } else {
            WriteCSVFile(filename, CSV, JDK, true, false);
        }
    }

    public static void WriteMethodCSVFile(String filename, String data, boolean JDK) throws Exception {
        WriteCSVFile(filename, data, JDK, false, true);
    }

    public static void WriteCSVFile(String filename, String data, boolean JDK, boolean clazz, boolean append) throws Exception {
        // JDK flag     ->  true: CSV for JDK;      false: CSV for Others
        // clazz flag   ->  true: CSV for class;    false: CSV for method
        String folder = getFolder(JDK, clazz);
        WriteFile(Paths.get(folder, Expanduser(filename)).toString(), data, append);
    }

    public static void WriteFile(String filePath, String data, boolean append) throws Exception {

        File file = new File(filePath);
        file.createNewFile();

        FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), append);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(data);
        bufferedWriter.close();
    }

    public static void ReplaceLineString(String filename, String original, String replacement) throws IOException {
        File file = new File(filename);
        String data = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        data = data.replace(original, replacement);
        FileUtils.writeStringToFile(file, data, StandardCharsets.UTF_8);
    }
}
