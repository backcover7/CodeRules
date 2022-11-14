package csv.resolver.taint;

import java.util.HashMap;

public interface CommonCSV {

    public HashMap<String, String> getCSVMap(String CSVline);

    public String SemorlizeCSV(String CSVline) throws Exception;
}
