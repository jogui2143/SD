import java.io.Serializable;

public class DepthControl implements Serializable{
  
  private String url;
  private int depth;

  public DepthControl(String url, int depth) {
    this.url = url;
    this.depth = depth;
  }

  public String getUrl() {
    return url;
  }

  public int getDepth() {
    return depth;
  }
  public void setDepth(int depth) {
    this.depth = depth;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
