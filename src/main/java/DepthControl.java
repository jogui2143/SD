import java.io.Serializable;
import java.util.Comparator;

/**
 * Represents a control object for managing the depth of URLs in a web crawling system.
 * This class implements the Serializable interface to support object serialization and the Comparator interface
 * to define a custom comparison logic for sorting DepthControl objects.
 */
public class DepthControl implements Serializable, Comparator<DepthControl> {

  private String url;
  private int depth;
  private long timestamp;

  /**
   * Constructs a DepthControl object with the specified URL and depth.
   * @param url The URL associated with this DepthControl object.
   * @param depth The depth of the URL in the crawling hierarchy.
   */
  public DepthControl(String url, int depth) {
      this.url = url;
      this.depth = depth;
  }

  /**
   * Gets the URL associated with this DepthControl object.
   * @return The URL.
   */
  public String getUrl() {
      return url;
  }

  /**
   * Gets the depth of the URL in the crawling hierarchy.
   * @return The depth.
   */
  public int getDepth() {
      return depth;
  }

  /**
   * Sets the depth of the URL in the crawling hierarchy.
   * @param depth The depth to set.
   */
  public void setDepth(int depth) {
      this.depth = depth;
  }

  /**
   * Gets the timestamp associated with this DepthControl object.
   * @return The timestamp.
   */
  public long getTimestamp() {
      return timestamp;
  }

  /**
   * Sets the timestamp associated with this DepthControl object.
   * @param timestamp The timestamp to set.
   */
  public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
  }

  /**
   * Compares two DepthControl objects based on their depth and timestamp.
   * @param o1 The first DepthControl object to compare.
   * @param o2 The second DepthControl object to compare.
   * @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
   */
  @Override
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
