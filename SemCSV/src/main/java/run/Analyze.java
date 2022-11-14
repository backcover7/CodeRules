package run;

import config.DbConfig;
import csv.resolver.analyzer.AnalyzeJDK;
import csv.resolver.analyzer.AnalyzeMaven;
import csv.resolver.analyzer.AnalyzeSoot;
import utils.ClasspathUtils;
import utils.DbUtils;
import utils.TableUtils;

import java.io.File;

public class Analyze {
    public static void main(String[] args) throws Exception {
        /* Analyze JDK Jar files */
//        AnalyzeJDK.getJDKCSV(false);

        /* Analyze dependency in maven pom.xml */
//        AnalyzeMaven.getMavenCSV(false);

        /* Analyze a collection of Jar files */
//        AnalyzeSoot.getJarsCSV(ClasspathUtils.SRC_MAIN_LIB, false,false);

        /* Create SQLite Database for all created CSVs */
        File db = new File(DbUtils.dbname);
        if (db.exists()) {
            db.delete();
        }
        new DbConfig();
        TableUtils.InitDB();

        DbConfig.connection.close();;
    }
}
