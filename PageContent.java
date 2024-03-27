import java.io.Serializable;

public class PageContent implements Serializable {

  private static final int MAX_TEXT_LENGTH = 500;

  private String title;
  private String text;
  private String url;
  private int numberOfReferences; // New field for number of references

  // Constructor
  public PageContent(String title, String text, String url, int numberOfReferences) {
      this.title = title;
      this.text = text;
      this.url = url;
      this.numberOfReferences = numberOfReferences;
  }

  public String getTitle(){
    return title;
  }

  public String getText(){
    return text;
  }

  public String getUrl(){
    return url;
  }

  public int getnumberOfRerences(){
    return numberOfReferences;
  }

  public String truncateText(String text) {
    if (text.length() > MAX_TEXT_LENGTH) {
        return text.substring(0, MAX_TEXT_LENGTH) + "...";
    }
    return text;}
}