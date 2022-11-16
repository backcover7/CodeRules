package parser.converter;

import utils.CharUtils;
import utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Mannual {
    private final String tag = "mannual";
    public final String csvDirectory = Paths.get(FileUtils.collections, this.tag).toString();

    public void process() {
        ArrayList<String> CsvFiles = FileUtils.getExtensionFiles(csvDirectory, CharUtils.CsvExtension, false);
        for (String csvFile : CsvFiles) {
            String WriteTarget = CharUtils.empty;
            if (Paths.get(csvFile).getFileName().toString().contains("source")) {
                WriteTarget = FileUtils.sources;
            } else if (Paths.get(csvFile).getFileName().toString().contains("sink")) {
                WriteTarget = FileUtils.sinks;
            }

            BufferedReader lineReader;
            try {
                lineReader = new BufferedReader(new FileReader(csvFile));

                String csv;
                while ((csv = lineReader.readLine()) != null) {
                    if (csv.isEmpty()) {
                        continue;
                    }
                    FileUtils.WriteFileLn(WriteTarget, csv, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
