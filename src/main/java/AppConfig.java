import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class AppConfig {
  private static final Properties properties = new Properties();
  private static final AppConfig instance = new AppConfig();
  public static AppConfig getInstance() {
    return instance;
  }
  public static String getProperty(String s) {
    return properties.getProperty(s);
  }

  private AppConfig() {
    try (FileInputStream fis = new FileInputStream("application.properties")) {
      properties.load(fis);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }


} 
    

