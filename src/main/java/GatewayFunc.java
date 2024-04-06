import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * Represents the functionality of a Gateway for managing URL indexing and search requests.
 * This class implements the GatewayInterface to provide methods for queuing URLs, retrieving new URLs,
 * searching URLs, and managing active barrels.
 */
public class GatewayFunc extends UnicastRemoteObject implements GatewayInterface {

    private static final int MAX_RETRIES = 3;
    private PriorityBlockingQueue<DepthControl> urlQueue = new PriorityBlockingQueue<>(100, new DepthControlComparator());
    private final ConcurrentHashMap<String, Integer> searchTermFrequencies = new ConcurrentHashMap<>();
    private Map<String, BarrelInterface> barrels = new ConcurrentHashMap<>();
    private Map<String, Integer> barrelPorts = new HashMap<>();
    private String currentBarrelKey = "localhost:1099";
    private List<String> barrelRegistryAddresses = new ArrayList<>();
    private Set<String> queuedUrls = new HashSet<>();

    /**
     * Constructs a GatewayFunc object.
     * Initializes necessary data structures and default settings.
     * @throws RemoteException if there is an issue with remote communication
     */
    public GatewayFunc() throws RemoteException {
        super();
        barrelPorts.put("localhost:1099", 1099);
        barrelPorts.put("localhost:1100", 1100);
        barrelRegistryAddresses.addAll(Arrays.asList("localhost:1099", "localhost:1100"));
    }
/**
 * Retrieves a BarrelInterface object corresponding to the provided key.
 * This method establishes a connection to the specified barrel and caches the connection for future use.
 * @param key The key identifying the barrel (format: host:port)
 * @return The BarrelInterface object associated with the given key
 * @throws RemoteException if there is an issue with remote communication
 */
private BarrelInterface getBarrel(String key) throws RemoteException {
    try {
        // Get the registry for the specified host and port
        Registry registry = LocateRegistry.getRegistry(key.split(":")[0], barrelPorts.get(key));
        
        // Look up the Barrel object from the registry
        BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
        
        // Cache the Barrel object for future use
        barrels.put(key, barrel);
        
        return barrel;
    } catch (Exception e) {
        // Remove the barrel from the cache if connection fails and throw RemoteException
        barrels.remove(key);
        throw new RemoteException("Connection to barrel failed: " + key, e);
    }
}
    /**
 * Retrieves the key of the next barrel in rotation.
 * This method returns the key of the next barrel in rotation, alternating between two predefined keys.
 * @return The key of the next barrel in rotation
 */
private synchronized String getNextBarrelKey() {
    // Toggle between two predefined barrel keys
    currentBarrelKey = currentBarrelKey.equals("localhost:1099") ? "localhost:1100" : "localhost:1099";
    return currentBarrelKey;
}

    /**
 * Retrieves a new URL from the queue.
 * This method retrieves and removes the next URL from the queue of URLs awaiting processing.
 * @return The next URL from the queue, or {@code null} if the queue is empty
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public DepthControl getNewUrl() throws RemoteException {
    return urlQueue.poll();
}

/**
 * Searches for information related to the given search term across all barrels.
 * This method attempts to search for information related to the specified search term across all barrels.
 * It retries the search operation with exponential backoff up to a maximum number of retries.
 * @param term The search term
 * @return A {@code ConcurrentSkipListSet} containing the search results, or an empty set if the search fails
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public ConcurrentSkipListSet<PageContent> searchInfo(String term) throws RemoteException {
    int attempts = 0;
    long backoff = 100;
    // Retry search operation with exponential backoff
    while (attempts < MAX_RETRIES) {
        // Get the next barrel key in round-robin fashion
        String barrelKey = getNextBarrelKey();
        try {
            // Get the barrel associated with the current key
            BarrelInterface barrel = getBarrel(barrelKey);
            // Record the search term frequency
            recordSearchTerm(term);
            // Perform search operation on the barrel
            return barrel.searchUrls(term);
        } catch (Exception e) {
            attempts++;
            try {
                // Apply exponential backoff before the next retry
                Thread.sleep(backoff + new Random().nextInt(500));
            } catch (InterruptedException ie) {
                // Interrupted while sleeping, propagate the exception
                Thread.currentThread().interrupt();
                throw new RemoteException("Thread interrupted during backoff", ie);
            }
            // Increase backoff for the next retry
            backoff *= 2;
        }
    }
    // Return an empty set if search fails after maximum retries
    return new ConcurrentSkipListSet<>();
}
    /**
 * Queues up a new URL for indexing if it meets the depth criteria and is not already queued.
 * 
 * <p>The method adds the URL to the priority queue for indexing if it meets the depth criteria 
 * (depth <= 2) and is not already present in the set of queued URLs. The URL's timestamp 
 * is set to the current system time before being added to the queue.
 * 
 * @param url The DepthControl object representing the URL to queue up
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public void queueUpUrl(DepthControl url) throws RemoteException {
    // Check if the URL meets depth criteria and is not already queued
    if (url.getDepth() <= 2 && !queuedUrls.contains(url.getUrl())) {
        // Set timestamp to current time
        url.setTimestamp(System.currentTimeMillis());
        // Add URL to the queue
        urlQueue.add(url);
        // Add URL to the set of queued URLs
        queuedUrls.add(url.getUrl());
    }
}

    /**
 * Searches for URLs that lead to a specific URL across multiple barrels.
 * 
 * <p>This method searches for URLs that lead to the specified URL across multiple barrels 
 * with retries in case of failure. It iterates over barrels, attempting to retrieve 
 * the search results. If the maximum number of retries is reached, an empty list is returned.
 * 
 * @param url The URL to search for
 * @return A list of URLs that lead to the specified URL, or an empty list if not found
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public List<String> searchURL(String url) throws RemoteException {
    int attempts = 0;
    long backoff = 100;
    while (attempts < MAX_RETRIES) {
        String barrelKey = getNextBarrelKey();
        try {
            // Retrieve barrel interface for the current key
            BarrelInterface barrel = getBarrel(barrelKey);
            // Search for URLs leading to the specified URL
            return barrel.searchURL(url);
        } catch (Exception e) {
            attempts++;
            try {
                // Apply exponential backoff for retries
                Thread.sleep(backoff + new Random().nextInt(500));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RemoteException("Thread interrupted during backoff", ie);
            }
            // Double the backoff time for next retry
            backoff *= 2;
        }
    }
    // Return an empty list if maximum retries are reached without success
    return new ArrayList<>();
}

    /**
 * Retrieves the top searched terms along with their frequencies.
 * 
 * <p>This method retrieves the top searched terms along with their frequencies from the gateway's
 * search term frequencies map. The terms are sorted based on their frequencies in descending order.
 * If there are more than 10 terms, only the top 10 terms are returned.
 * 
 * @return A list containing the top searched terms along with their frequencies
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public List<String> getTopSearchedTerms() throws RemoteException {
    // Sort the search term frequencies map entries based on values (frequencies)
    List<Map.Entry<String, Integer>> sortedTerms = searchTermFrequencies.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toList());

    // Limit the list to the top 10 terms and format them as "term: frequency"
    return sortedTerms.stream()
            .limit(10)
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.toList());
}


    /**
 * Retrieves information about active barrels.
 * 
 * <p>This method retrieves information about active barrels by querying each barrel in the registry.
 * It iterates through the list of barrel registry addresses, attempts to connect to each barrel,
 * and retrieves the barrel information using the {@code getBarrelInfo()} method. The information
 * includes the barrel ID and average search time. Any exceptions that occur during the process are
 * logged, and the method continues to query the remaining barrels.
 * 
 * @return A set containing information about active barrels
 * @throws RemoteException If a remote communication error occurs
 */
@Override
public Set<String> getActiveBarrels() throws RemoteException {
    // Initialize a set to store information about active barrels
    Set<String> activeBarrelInfo = new HashSet<>();
    
    // Iterate through the list of barrel registry addresses
    for (String registryAddress : barrelRegistryAddresses) {
        try {
            // Get the registry and look up the barrel interface
            Registry registry = LocateRegistry.getRegistry(registryAddress.split(":")[0],
                    Integer.parseInt(registryAddress.split(":")[1]));
            BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
            
            // Add the barrel information to the set
            activeBarrelInfo.add(barrel.getBarrelInfo());
        } catch (Exception e) {
            // Log any exceptions that occur during the process
            System.err.println("Failed to connect to barrel at " + registryAddress + ": " + e.toString());
        }
    }
    return activeBarrelInfo;
}


   /**
 * Records the frequency of a search term.
 * 
 * <p>This method records the frequency of a search term by updating the map {@code searchTermFrequencies}.
 * If the term already exists in the map, its frequency is incremented by 1; otherwise, a new entry with
 * a frequency of 1 is added to the map.
 * 
 * @param term The search term to be recorded
 */
private void recordSearchTerm(String term) {
    searchTermFrequencies.merge(term, 1, Integer::sum);
}
}
