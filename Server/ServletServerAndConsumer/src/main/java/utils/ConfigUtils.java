package utils;

import model.Configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigUtils {
    private static final String FILE_NAME = "config.properties";

    // Static path for servlet deployments (e.g., on EC2)
    public static String EXTERNAL_CONFIG_PATH = "/home/ec2-user/config.properties";

    public static Configuration getConfigurationForService() {
        try {
            // 1. Try loading from current working directory
            if (Files.exists(Paths.get(FILE_NAME))) {
                System.out.println("[Config] Loading config from current directory");
                try (InputStream input = Files.newInputStream(Paths.get(FILE_NAME))) {
                    Properties props = new Properties();
                    props.load(input);
                    return new Configuration(props);
                }
            }

            // 2. Fallback to classpath
            System.out.println("[Config] Loading config from classpath");
            try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (input != null) {
                    Properties props = new Properties();
                    props.load(input);
                    return new Configuration(props);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Configuration(); // fallback to default
    }

    public static Configuration getConfigurationForServlet(ServletContext context) throws ServletException {
        try {
            // 1. Try loading from external static path (EC2-specific)
            if (Files.exists(Paths.get(EXTERNAL_CONFIG_PATH))) {
                System.out.println("[Config] Loading servlet config from external path: " + EXTERNAL_CONFIG_PATH);
                try (InputStream input = Files.newInputStream(Paths.get(EXTERNAL_CONFIG_PATH))) {
                    Properties props = new Properties();
                    props.load(input);
                    return new Configuration(props);
                }
            }

            // 2. Fallback to WEB-INF/classes/
            System.out.println("[Config] Loading servlet config from WEB-INF/classes");
            try (InputStream input = context.getResourceAsStream("/WEB-INF/classes/" + FILE_NAME)) {
                if (input != null) {
                    Properties props = new Properties();
                    props.load(input);
                    return new Configuration(props);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Configuration(); // fallback to default
    }
}
