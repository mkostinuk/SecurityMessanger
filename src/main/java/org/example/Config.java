package org.example;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new RuntimeException("could not load config.properties", e);
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(PROPS.getProperty(key));
    }
}
