package parser.converter;

import org.springframework.web.servlet.tags.EvalTag;
import utils.CharUtils;
import utils.FileUtils;

import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Mannual {
    private final String tag = "nodes";
    public final String csvDirectory = Paths.get(FileUtils.csv, this.tag).toString();

    public void process() {
        ArrayList<String> CsvFiles = FileUtils.getExtensionFiles(csvDirectory, CharUtils.CsvExtension, false);
        for (String csvFile : CsvFiles) {

            BufferedReader lineReader;
            try {
                lineReader = new BufferedReader(new FileReader(csvFile));

                String csv;
                while ((csv = lineReader.readLine()) != null) {
                    if (csv.isEmpty()) {
                        continue;
                    }
                    String[] csvArray = csv.split(CharUtils.colon);
                    String classname = String.join(CharUtils.dot, csvArray[CSVDefinition.NAMESPACEINDEX], csvArray[CSVDefinition.CLASSTYPEINDEX]);

                    try {
                        if (Arrays.asList(new String[] {
                                "org.springframework.scripting.bsh", "org.springframework.web.servlet.tags"
                        }).contains(csvArray[CSVDefinition.NAMESPACEINDEX])) {
                            continue;
                        }

                        Class<?> clazz = Class.forName(classname);
                        if (!clazz.getSuperclass().equals(Object.class)) {
                            System.out.println("[!] Class not found: " + classname);
                        }
                    } catch (Exception e) {
                        System.out.println("[!] Class has super class: " + classname);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Object a = javax.naming.directory.DirContext.class.getMethods();
        new Mannual().process();
    }
}
