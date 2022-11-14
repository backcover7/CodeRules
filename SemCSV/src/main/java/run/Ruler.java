package run;

import config.DbConfig;
import csv.resolver.taint.CodeQLCSV;
import csv.resolver.taint.FindsecbugsCSV;

public class Ruler {
    public static void main(String[] args) throws Exception {
        new DbConfig();

        /* Write rules from 3pp sink nodes and meantime backup them to semgrep source/sink csv */
        new CodeQLCSV().WriteCodeQLYaml();
        new FindsecbugsCSV().WriteFindsecbugsYaml();

        DbConfig.connection.close();;
    }
}
