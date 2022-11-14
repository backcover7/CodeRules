package run;

import utils.ClasspathUtils;
import org.apache.commons.io.FileUtils;
import utils.CsvUtils;
import utils.FilesUtils;

import java.io.File;
import java.io.IOException;

public class Clean {
    public static void main(String[] args) throws IOException {
        FileUtils.cleanDirectory(new File(ClasspathUtils.Dependencies));

        FileUtils.cleanDirectory(new File(FilesUtils.JDK_class_csv));
        FileUtils.cleanDirectory(new File(FilesUtils.JDK_method_csv));
        FileUtils.cleanDirectory(new File(FilesUtils.Other_class_csv));
        FileUtils.cleanDirectory(new File(FilesUtils.Other_method_csv));
    }
}
