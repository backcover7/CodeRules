package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtils {
    public static String StdoutProcess(Process process) throws IOException {
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = stdInput.readLine()) != null) {
            stringBuilder.append(line).append(CharUtils.LF);
        }
        return stringBuilder.toString();
    }
}
