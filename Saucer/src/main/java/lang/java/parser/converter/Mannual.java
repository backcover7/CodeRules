package lang.java.parser.converter;

import utils.CharUtils;
import utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Mannual {
    private final static String tag = "nodes";
    public final static String csvDirectory = Paths.get(FileUtils.csv, tag).toString();

    public void process() {
        String csvFile = Paths.get(csvDirectory, "backuptest.csv").toString();
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
                        Class<?> clz = clazz.getSuperclass();
                        Method[] methods = clz.getMethods();
                        for (Method method : methods) {
                            if (method.getName().equals(csvArray[CSVDefinition.METHODINDEX])) {
//                                System.out.println("[!] Class has super class: " + classname + ", method name: " + csvArray[CSVDefinition.METHODINDEX]);
                            }
                        }
                    } else if (clazz.getInterfaces().length != 0) {
                        Class<?>[] clzzes = clazz.getInterfaces();
                        for (Class<?> claxx : clzzes) {
                            Method[] methods = claxx.getMethods();
                            for (Method method : methods) {
                                if (method.getName().equals(csvArray[CSVDefinition.METHODINDEX])) {
//                                    System.out.println("[!] Class has interface: " + classname + ", method name: " + csvArray[CSVDefinition.METHODINDEX]);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[!] Class not found: " + classname+ ", method name: " + csvArray[CSVDefinition.METHODINDEX]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Object a = javax.naming.directory.DirContext.class.getMethods();
        new Mannual().process();
    }
}
