import java.io.Serializable;

public class PageContent implements Serializable {
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
}