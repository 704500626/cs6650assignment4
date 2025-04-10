package utils;

import model.Configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {
    public static Configuration getConfigurationForConsumer() {
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return new Configuration();
            }
            Properties properties = new Properties();
            properties.load(input);
            return new Configuration(properties);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new Configuration();
    }

    public static Configuration getConfigurationForServlet(ServletContext context) throws ServletException {
        // Load properties file from /WEB-INF/classes/
        try (InputStream input = context.getResourceAsStream("/WEB-INF/classes/config.properties")) {
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
