import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The <code>BarrelFunc</code> class implements the <code>BarrelInterface</code>
 * and provides
 * functionality for searching URLs and retrieving information about a Barrel
 * instance.
 * It extends <code>UnicastRemoteObject</code> to enable remote method
 * invocation (RMI).
 * This class represents a functional component of the Barrel system.
 */
public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private UUID id;
    private ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index;
    private ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex;
    private List<Long> searchTimes = Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructs a <code>BarrelFunc</code> object with the specified ID, pages
     * index, and URL hyperlink index.
     * 
     * @param id                The UUID of the BarrelFunc instance.
     * @param pages             The index of pages stored in the Barrel.
     * @param urlHyperlinkIndex The index of URLs and their hyperlinks.
     * @throws RemoteException If a remote communication error occurs.
     */
    public BarrelFunc(UUID id, ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> pages,
            ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex) throws RemoteException {
        super();
        this.id = id;
        this.index = pages;
        this.urlHyperlinkIndex = urlHyperlinkIndex;
    }

    /**
     * Searches for URLs containing the specified term.
     * This method performs a case-insensitive search for URLs containing the given
     * term.
     * It splits the term into words, retrieves corresponding sets of PageContent
     * objects
     * from the index, and intersects them to find common URLs.
     * The search is optimized by early termination if no common URLs are found.
     * 
     * @param term The search term.
     * @return A <code>ConcurrentSkipListSet</code> containing matching
     *         <code>PageContent</code> objects.
     *         If no matching URLs are found, an empty set is returned.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException {
        // Start measuring execution time
        long startTime = System.nanoTime();

        // Split the search term into words
        String[] words = term.toLowerCase().split("\\s+");

        // Initialize a list to store sets of PageContent objects corresponding to each
        // word
        List<ConcurrentSkipListSet<PageContent>> listOfSets = new ArrayList<>();
          System.out.println("lalalalalal");
        // Iterate through each word in the search term
        for (String word : words) {
            // If the index contains the word, add its corresponding set to the list
            if (index.containsKey(word)) {
                listOfSets.add(index.get(word));
            } else {
                // If the word is not found in the index, return an empty set
                return new ConcurrentSkipListSet<>();
            }
        }

        // Initialize the result set with the first set from the list
        Iterator<ConcurrentSkipListSet<PageContent>> it = listOfSets.iterator();
        ConcurrentSkipListSet<PageContent> results = new ConcurrentSkipListSet<>(it.next());

        // Intersect the sets to find common URLs
        while (it.hasNext()) {
            results.retainAll(it.next());
            // If the result set becomes empty, terminate the search
            if (results.isEmpty()) {
                return results;
            }
        }

        // End measuring execution time and record the search time
        long endTime = System.nanoTime();
        searchTimes.add((endTime - startTime) / 100000);

        // Return the result set
        return results;
    }

    /**
     * Searches for URLs containing the specified hyperlink.
     * This method iterates over the URL hyperlink index to find URLs containing the
     * queried hyperlink.
     * It returns a list of URLs that contain the specified hyperlink.
     * 
     * @param hyperlink The hyperlink to search for.
     * @return A list of URLs containing the specified hyperlink.
     * @throws RemoteException If a remote communication error occurs.
     */
    public List<String> searchURL(String hyperlink) throws RemoteException {
        // Start measuring execution time
        long startTime = System.nanoTime();

        // Initialize the result list
        List<String> result = new ArrayList<>();

        // Iterate over the URL hyperlink index to find matching URLs
        for (Map.Entry<String, Set<String>> entry : urlHyperlinkIndex.entrySet()) {
            // Check if the current URL contains the queried hyperlink
            if (entry.getValue().contains(hyperlink)) {
                // Add the URL to the result list
                result.add(entry.getKey());
            }
        }

        // End measuring execution time and record the search time
        long endTime = System.nanoTime();
        searchTimes.add((endTime - startTime) / 100_000_000);

        // Return the result list
        return result;
    }

    /**
     * Retrieves information about the BarrelFunc instance.
     * This method calculates the average search time based on the recorded search
     * times.
     * It then constructs a string containing the ID of the BarrelFunc instance and
     * its average search time.
     * 
     * @return A string containing the ID and average search time of the BarrelFunc
     *         instance.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public String getBarrelInfo() throws RemoteException {
        // Declare a variable to store the average search time
        double averageTime;

        // Calculate the average search time
        synchronized (searchTimes) {
            averageTime = searchTimes.isEmpty() ? 0.0
                    : searchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        // Construct and return the information string
        return "ID: " + this.id.toString() + ", Avg Time: " + averageTime + " deciseconds";
    }

    /**
     * Retrieves the ID of the BarrelFunc instance.
     * This method returns the UUID of the BarrelFunc instance.
     * Additionally, it prints a message indicating the ID being returned.
     * 
     * @return The UUID of the BarrelFunc instance.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public UUID getId() throws RemoteException {
        // Print a message indicating the ID being returned
        System.out.println("Returning ID: " + this.id);

        // Return the UUID of the BarrelFunc instance
        return this.id;
    }

}
