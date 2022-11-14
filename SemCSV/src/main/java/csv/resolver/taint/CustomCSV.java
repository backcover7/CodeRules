package csv.resolver.taint;

import utils.CsvUtils;
import utils.StringsUtils;

import java.util.HashMap;
import java.util.HashSet;

public class CustomCSV extends SemCSV {
    public final static String CsvType = "custom";

    public HashMap<String, String> getCSVMap(String CSVline) {
        return getCSVMap(CSVline, StringsUtils.empty);
    }

    public HashMap<String, String> getCSVMap(String CSVline, String kind) {
        return CsvUtils.getFunctionInfoMap(CSVline, kind);
    }
}
