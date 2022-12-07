package com.saucer.sast.lang.javascript;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.SemgrepUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DependencyConfusion implements VulnScan {
    private final static String dependency_confusion_yaml =
            Paths.get(SemgrepUtils.SemgrepJavascriptSinkRules, "dependency_confusion.yaml").toString();

    private final static int NameIndex = 0;
    private final static int VersionIndex = 1;

    private final static String DEPS_METAVAR = "$DEPS";
    private final static String VERSION_METAVAR = "$VERSION";

    private final static String OrgCheckURL = "https://www.npmjs.com/org/${org}";
//    private final static String OrgVersionCheckURL = "https://www.npmjs.com/package/@${org}/${package}/v/${version}";
    private final static String PackageWoOrgCheckURL = "https://www.npmjs.com/package/${package}";
//    private final static String PackageVersionWoOrgCheckURL = "https://www.npmjs.com/package/${package}/v/${version}";

    private boolean FlagOrgDoesNotExists = false;
//    private boolean FlagOrgVersionDoesNotExists = false;
    private boolean FlagPckDoesNotExists = false;
//    private boolean FlagPckVersionDoesNotExists = false;

    public void Scan(String codebase) throws IOException, InterruptedException {
        ArrayList<HashMap<String, Object>> resultList = SemgrepUtils.RunSemgrepRule(dependency_confusion_yaml, codebase);
        log.info("[.] Running Dependency Confusion check...");
        for (HashMap<String, Object> result : resultList) {
            CheckVuln(result);     // Example: "@rollup/plugin-replace": "^2.2.0"
            reportContent(result);
        }
    }

    public void reportContent(HashMap<String, Object> result) {
        String position = SemgrepUtils.ReportPosition(result);
        String lines = ((String) result.get(SemgrepUtils.Lines)).trim();
        String found = "[+] Found Dependency Confusion vulnerability!";
        if (FlagOrgDoesNotExists) {
            log.info(String.join(CharUtils.space,found, lines, position, "The organization could be registered!"));
            FlagOrgDoesNotExists = false;
        } else if (FlagPckDoesNotExists) {
            log.info(String.join(CharUtils.space,found, lines, position, "The package does not exist!"));
            FlagPckDoesNotExists = false;
        }
    }

    private void CheckVuln(HashMap<String, Object> result) throws IOException, InterruptedException {
        Map<String, String> dependencyMap = ProcessDependency(result);
        StringSubstitutor stringSubstitutor = new StringSubstitutor(dependencyMap);
        if (dependencyMap.get("org") != null) {
            CheckPackageWithOrg(stringSubstitutor);
        } else {
            CheckPackageWoOrgExistence(stringSubstitutor);
        }
    }

    private Map<String, String> ProcessDependency(HashMap<String, Object> result) {
        HashMap<String, Object> metavarsMap = (HashMap<String, Object>) result.get(SemgrepUtils.Metavars);
        String depName = ((HashMap<String, String>) metavarsMap.get(DEPS_METAVAR)).get(SemgrepUtils.Abstract_Content);
        String depVersion = ((HashMap<String, String>) metavarsMap.get(VERSION_METAVAR)).get(SemgrepUtils.Abstract_Content);

        DependencyProperty dependencyProperty = new DependencyProperty();
        dependencyProperty.setVersion(depVersion);
        if (depName.contains(CharUtils.at)) {
            String[] OrgAndPackage = depName.split(CharUtils.slash);
            dependencyProperty.setOrg(OrgAndPackage[0].replaceAll(CharUtils.at, CharUtils.empty));
            dependencyProperty.setPackage(OrgAndPackage[1]);
        } else {
            dependencyProperty.setOrg(null);
            dependencyProperty.setPackage(depName);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(dependencyProperty, Map.class);
    }

    private void CheckPackageWithOrg(StringSubstitutor stringSubstitutor) throws IOException, InterruptedException {
        boolean orgExistence = CheckWithURL(stringSubstitutor.replace(OrgCheckURL));
        if (!orgExistence) {
            FlagOrgDoesNotExists = true;
        }
//        else {
//            boolean orgVerionExistence = CheckWithURL(StringSubstitutor.replace(OrgVersionCheckURL, dependencyMap));
//            if (!orgVerionExistence) {
//                FlagOrgVersionDoesNotExists = true;
//            }
//        }
    }

    private void CheckPackageWoOrgExistence(StringSubstitutor stringSubstitutor) throws IOException, InterruptedException {
        boolean pckExistence = CheckWithURL(stringSubstitutor.replace(PackageWoOrgCheckURL));
        if (!pckExistence) {
            FlagPckDoesNotExists = true;
        }
//        else {
//            boolean pckVerionExistence = CheckWithURL(stringSubstitutor.replace(PackageVersionWoOrgCheckURL, dependencyMap));
//            if (!pckVerionExistence) {
//                FlagPckVersionDoesNotExists = true;
//            }
//        }
    }

    private boolean CheckWithURL(String endpoint) throws IOException, InterruptedException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int status = connection.getResponseCode();
        if (status == 200) {
            return true;
        } else if (status == 404) {
            return false;
        } else {
//            System.out.println("[!] Too fast! Waiting for 5 seconds...");
            Thread.sleep(10000);
            return CheckWithURL(endpoint);
        }
    }
}

class DependencyProperty {
    private String Org;
    private String Package;

    public String getOrg() {
        return Org;
    }

    public void setOrg(String org) {
        Org = org;
    }

    public String getPackage() {
        return Package;
    }

    public void setPackage(String aPackage) {
        Package = aPackage;
    }

    public String getVersion() {
        return Version;
    }

    public void setVersion(String version) {
        Version = version;
    }

    private String Version;
}