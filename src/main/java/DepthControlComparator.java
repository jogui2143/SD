import java.util.Comparator;

public class DepthControlComparator implements Comparator<DepthControl> {
    @Override
    public int compare(DepthControl o1, DepthControl o2) {
        int depthCompare = Integer.compare(o1.getDepth(), o2.getDepth());
        if (depthCompare != 0) {
            return depthCompare;
        }
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
}