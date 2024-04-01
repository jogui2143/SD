import java.io.Serializable;
import java.util.List;

public class PageContent implements Serializable {

  private static final int MAX_TEXT_LENGTH = 500;

  private String title;
  private String text;
  private String url;
  private List<String> references; // New field for number of references

  // Constructor
  public PageContent(String title, String text, String url, List<String> references) {
      this.title = title;
      this.text = text;
      this.url = url;
      this.references = references;
      if(this.references == null){
          this.references = List.of();
      }
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

  public List<String> getReferences(){
    return references;
  }

  public int getNumberOfReferences(){
    return references.size();
  }

  public String truncateText(String text) {
    if (text.length() > MAX_TEXT_LENGTH) {
        return text.substring(0, MAX_TEXT_LENGTH) + "...";
    }
    return text;}


    @Override
    public String toString() {
        return "PageContent{" +
               "title='" + title + '\'' +
               ", url='" + url + '\'' +
               ", text='" + truncateText(text) + '\'' +
               ", numberOfReferences=" + getNumberOfReferences() +
               '}';
    }
}