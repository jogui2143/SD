import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * The AppConfig class is responsible for managing application configuration properties.
 * It provides a singleton instance to access these properties and retrieve values based on keys.
 * The configuration properties are loaded from the "application.properties" file.
 */
public class AppConfig {
  
  // Properties object to store configuration properties
  private static final Properties properties = new Properties();
  
  // Singleton instance of AppConfig
  private static final AppConfig instance = new AppConfig();
  
  /**
   * Private constructor to prevent external instantiation.
   * Loads the configuration properties from the "application.properties" file.
   */
  private AppConfig() {
    try (FileInputStream fis = new FileInputStream("application.properties")) {
      properties.load(fis);
    } catch (IOException ex) {
      // Print stack trace in case of any IO exceptions
      ex.printStackTrace();
    }
  }
  
  /**
   * Returns the singleton instance of AppConfig.
   * @return The singleton instance of AppConfig.
   */
  public static AppConfig getInstance() {
    return instance;
  }
  
  /**
   * Retrieves the value of the property associated with the specified key.
   * @param key The key of the property.
   * @return The value of the property associated with the specified key, or null if the key is not found.
   */
  public static String getProperty(String key) {
    return properties.getProperty(key);
  }
}

    

