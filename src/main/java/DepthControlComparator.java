import java.util.Comparator;

/**
 * Comparator implementation for comparing DepthControl objects based on their depth and timestamp.
 * This class implements the Comparator interface to define a custom comparison logic for sorting DepthControl objects.
 */
public class DepthControlComparator implements Comparator<DepthControl> {

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
