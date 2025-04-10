package edu.northeastern.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerUtils {
    public static String getServerUrl() {
        Properties properties = new Properties();
        String serverUrl = "/";
        try (InputStream input = ServerUtils.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
            }
            properties.load(input);
            String local = properties.getProperty("local");
            String springboot = properties.getProperty("springboot");
            if (local.equals("true")) {
                if (springboot.equals("true")) {
                    serverUrl = properties.getProperty("local.springboot.url");
                } else {
                    serverUrl = properties.getProperty("local.servlet.url");
                }
            } else {
                if (springboot.equals("true")) {
                    serverUrl = properties.getProperty("remote.springboot.url");
                } else {
                    serverUrl = properties.getProperty("remote.servlet.url");
                }
            }
            System.out.println("Server URL: " + serverUrl);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return serverUrl;
    }
}
