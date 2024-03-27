import java.io.Serializable;

public class PageContent implements Serializable {

  private static final int MAX_TEXT_LENGTH = 500;

  private String title;
  private String text;
  private String url;




  


  public PageContent(String title, String text, String url){
    this.title = title;
    this.text = text;
    this.url = url;
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

  public String truncateText(String text) {
    if (text.length() > MAX_TEXT_LENGTH) {
        return text.substring(0, MAX_TEXT_LENGTH) + "...";
    }
    return text;}
}