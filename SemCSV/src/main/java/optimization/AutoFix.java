package optimization;

import config.DbConfig;
import utils.FilesUtils;
import utils.DbUtils;
import utils.RuleUtils;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;

public class AutoFix {
    private String codebase;

    public AutoFix(String codebase) {
        this.codebase = FilesUtils.Expanduser(codebase);
    }

    public void RemoveJavaLang() throws IOException {
        Runtime.getRuntime().exec(
                ArrayUtils.addAll(RuleUtils.SemgrepCLI, RuleUtils.java_lang, RuleUtils.EnableFixFlag, codebase));
    }

    public void ExplicitImportStarStmt() throws Exception {
        Process process = Runtime.getRuntime().exec(
                ArrayUtils.addAll(RuleUtils.SemgrepCLI, RuleUtils.import_star_stmt, RuleUtils.EMACS, codebase));

        ArrayList<HashMap<String, String>> ResultList = RuleUtils.ProcessEMACSResult(process);
        for (HashMap<String, String> res: ResultList) {
            String MatchCode = res.get(RuleUtils.MatchCode);
            String ImportReplacement = RuleUtils.QueryImportReplacement(RuleUtils.ExtractNamespace(MatchCode));
            FilesUtils.ReplaceLineString(res.get(RuleUtils.FilePath), MatchCode, ImportReplacement);
        }
        DbConfig.connection.close();
    }

    public static void main(String[] args) throws Exception {
        AutoFix autoFix = new AutoFix("~/Downloads/x.java");
        autoFix.RemoveJavaLang();
        autoFix.ExplicitImportStarStmt();
    }
}