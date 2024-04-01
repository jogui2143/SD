import java.io.Serializable;
import java.util.List;

public class PageContent implements Serializable, Comparable<PageContent> {

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

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getReferences() {
    return references;
  }

  public int getNumberOfReferences() {
    if (this.references == null) {
      return 0;
    }

    return references.size();
  }

  public String truncateText(String text) {
    if (text.length() > MAX_TEXT_LENGTH) {
      return text.substring(0, MAX_TEXT_LENGTH) + "...";
    }
    return text;
  }

  @Override
  public String toString() {
    return "PageContent{" +
        "title='" + title + '\'' +
        ", url='" + url + '\'' +
        ", text='" + truncateText(text) + '\'' +
        ", numberOfReferences=" + getNumberOfReferences() +
        '}';
  }

  public int compareTo(PageContent other) {
    // Comparing based on the number of references, larger numbers first.
    int thisSize = this.references != null ? this.references.size() : 0;
    int otherSize = other.references != null ? other.references.size() : 0;
    return Integer.compare(otherSize, thisSize); // Note the order is reversed here
  }
}