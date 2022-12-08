package com.saucer.sast.lang.java.config;

import java.util.Properties;

public class PropertyConfig {
    private Properties properties = new Properties();

    public static String RULES = "rules";
    public static String DEPENDENCY = "dependency";
    public static String FLOW = "flow";
    public static String OUTPUT = "output";

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
