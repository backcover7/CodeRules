package csv.resolver.analyzer;

import config.SootConfiguration;
import csv.resolver.info.ExtractorFromSoot;
import csv.resolver.info.FunctionInfo;
import csv.resolver.taint.SemCSV;
import soot.*;
import utils.ClasspathUtils;
import utils.CsvUtils;
import utils.FilesUtils;
import utils.StringsUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AnalyzeSoot {
    public static void getJarsCSV(String JarsPath, boolean print, boolean JDK) throws Exception {
        SootConfiguration.initSootOption();
        List<String> targets = FilesUtils.getExtensionFiles(
                JarsPath, StringsUtils.JarExtension, true);
        Scene.v().setSootClassPath(String.join(File.pathSeparator, ClasspathUtils.getSootClassPath(targets)));
        Scene.v().loadBasicClasses();
        Scene.v().loadDynamicClasses();

        for (String target : targets) {   // Every jar

            Path path = Paths.get(target).getFileName();
            String filename = path.toString().replaceAll(StringsUtils.JarExtension, StringsUtils.CsvExtension);
            String classCSV = StringsUtils.empty;        // Class CSVs
            String methodFolder = FilesUtils.getFolder(JDK, false);
            List<String> cls = SourceLocator.v().getClassesUnder(target);

            if (print || FilesUtils.FileExistingPrompt(methodFolder, filename, StringsUtils.MethodCSV)) {
                for (String cl : cls) { // Every class
                    try {
                        SootClass theClass = Scene.v().loadClassAndSupport(cl);
                        ArrayList<FunctionInfo> functionInfos = new ExtractorFromSoot(theClass).InitFromSoot();

                        classCSV += CsvUtils.getClassCSVFromSootClass(theClass, JDK) + StringsUtils.LF;
                        String methodCSV = CsvUtils.getMethodCSVFromSootClass(functionInfos, JDK);
                        FilesUtils.PrintOrWriteMethodCSV(methodCSV, print, filename, JDK);

                        theClass.setApplicationClass();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("[+] Method CSV of " + path + " is Done!");
            }

            String clazzFolder = FilesUtils.getFolder(JDK, true);
            if (print || FilesUtils.FileExistingPrompt(clazzFolder, filename, StringsUtils.ClassCSV)) {
                FilesUtils.PrintOrWriteClassCSV(classCSV, print, filename, JDK);
                System.out.println("[+] Class CSV of " + path + " is Done!");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        getJarsCSV(ClasspathUtils.Dependencies, true, false);
    }
}
