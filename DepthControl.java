import java.io.Serializable;
import java.util.Comparator;

public class DepthControl implements Serializable,Comparator<DepthControl>{
  
  private String url;
  private int depth;
  private long timestamp;

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

  public long getTimestamp() {
    return timestamp;
}

public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
}

  public int compare(DepthControl o1, DepthControl o2) {
    // First, compare by depth
    int depthCompare = Integer.compare(o1.getDepth(), o2.getDepth());
    if (depthCompare != 0) {
        return depthCompare;
    }
    // If depths are equal, compare by timestamp (earlier has higher priority)
    return Long.compare(o1.getTimestamp(), o2.getTimestamp());
}
}
