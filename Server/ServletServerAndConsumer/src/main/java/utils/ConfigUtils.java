package utils;

import model.Configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {
    private static final String FILE_NAME = "config.properties";

    public static Configuration getConfigurationForService() {
        // First, try to load config.properties from the current working directory
        File localFile = new File(FILE_NAME);
        if (localFile.exists()) {
            try (InputStream input = new FileInputStream(localFile)) {
                Properties properties = new Properties();
                properties.load(input);
                System.out.println("[ConfigUtils] Loaded config.properties from working directory.");
                return new Configuration(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Then read from classpath
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (input == null) {
                System.out.println("[ConfigUtils] config.properties not found in classpath.");
                return new Configuration();
            }
            Properties properties = new Properties();
            properties.load(input);
            System.out.println("[ConfigUtils] Loaded config.properties from classpath.");
            return new Configuration(properties);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new Configuration();
    }

    public static Configuration getConfigurationForServlet(ServletContext context) throws ServletException {
        // First, try to load from the working directory
        File localFile = new File(FILE_NAME);
        if (localFile.exists()) {
            try (InputStream input = new FileInputStream(localFile)) {
                Properties configProperties = new Properties();
                configProperties.load(input);
                System.out.println("[ConfigUtils] Loaded config.properties from working directory.");
                return new Configuration(configProperties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load properties file from /WEB-INF/classes/
        try (InputStream input = context.getResourceAsStream("/WEB-INF/classes/" + FILE_NAME)) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return new Configuration();
            }
            Properties configProperties = new Properties();
            configProperties.load(input);
            return new Configuration(configProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Configuration();
    }
}
