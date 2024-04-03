
// Importing necessary Java RMI and concurrency classes.
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

// GatewayFunc class declaration, extending UnicastRemoteObject to enable RMI, and implementing GatewayInterface.
public class GatewayFunc extends UnicastRemoteObject implements GatewayInterface {
    // A concurrent queue to store URLs. ConcurrentLinkedQueue ensures thread-safe
    // operations.
    private PriorityBlockingQueue<DepthControl> urlQueue = new PriorityBlockingQueue<>(100,
            new DepthControlComparator());
    private final ConcurrentHashMap<String, Integer> searchTermFrequencies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> searchURLCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> searchInfoCache = new ConcurrentHashMap<>();
    private static final int MAX_RETRIES = 3; // Maximum number of retry attempts
    // Comment: "Queue of URLs, I guess :)" (presumably a note from the developer).

    // Constructor for GatewayFunc.
    private Map<String, BarrelInterface> barrels = new ConcurrentHashMap<>();
    private Map<String, Integer> barrelPorts = new HashMap<>();
    private String currentBarrelKey = "localhost:1099"; // Default starting barrel
    private List<String> barrelRegistryAddresses = new ArrayList<>();

    // Constructor for GatewayFunc.
    public GatewayFunc() throws RemoteException {
        super(); // Calling the constructor of UnicastRemoteObject.
        // Initialize the ports for each barrel
        barrelPorts.put("localhost:1099", 1099);
        barrelPorts.put("localhost:1100", 1100);
        barrelRegistryAddresses.add("localhost:1099");
        barrelRegistryAddresses.add("localhost:1100");
    }
    private BarrelInterface getBarrel(String key) throws RemoteException {
        try {
            // Attempt to get the barrel every time the method is called, regardless of the cache
            System.out.println("[Debug] Attempting to connect to barrel: " + key);
            String[] parts = key.split(":");
            Registry registry = LocateRegistry.getRegistry(parts[0], barrelPorts.get(key));
            BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
    
            // If connection is successful, update the cache and return the barrel
            barrels.put(key, barrel);
            System.out.println("[Debug] Current size of barrels cache: " + barrels.size());

            System.out.println("[Debug] Connected to barrel: " + barrel.getId());
            return barrel;
        } catch (Exception e) {
            // Remove from cache if connection failed
            barrels.remove(key);
            System.err.println("[Debug] Connection to barrel failed, removed from cache: " + key);
            throw new RemoteException("Connection to barrel failed: " + key, e);
        }
    }

    private synchronized String getNextBarrelKey() {
        // Assume a simple round-robin key switching mechanism
        currentBarrelKey = currentBarrelKey.equals("localhost:1099") ? "localhost:1100" : "localhost:1099";
        System.out.println("fethinc"+currentBarrelKey);
        return currentBarrelKey;
    }

    public void recordSearchTerm(String term) {
        searchTermFrequencies.merge(term, 1, Integer::sum);
    }


   

   

    // Overriding the getNewUrl method from GatewayInterface.
    @Override
    public DepthControl getNewUrl() throws RemoteException {
        // Polls and returns the head of the URL queue, or null if the queue is empty.
        //System.out.println("" + urlQueue.size() + " URLs in the queue");
        return urlQueue.poll();
    }

    // Overriding the searchinfo method from GatewayInterface.
    @Override
public ConcurrentSkipListSet<PageContent> searchinfo(String term) throws RemoteException {
    int attempts = 0;
    long backoff = 1000; // Starting with 1 second.

    while (attempts < MAX_RETRIES) {
        String barrelKey = getNextBarrelKey();
        try {
            
            BarrelInterface barrel = getBarrel(barrelKey);
            recordSearchTerm(term);
            ConcurrentSkipListSet<PageContent> result = barrel.searchUrls(term);
            searchInfoCache.put(term, new ConcurrentSkipListSet<>(result));
            System.out.println(" with Barrel " + barrelKey);
            return result; // If successful, return the result immediately.
        } catch (Exception e) {
            System.err.println("Attempt " + attempts + " with Barrel " + barrelKey + " failed: " + e.toString());
            attempts++;
            try {
                Thread.sleep(backoff + new Random().nextInt(500)); // Wait with randomization
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RemoteException("Thread interrupted during backoff", ie);
            }
            backoff *= 2; // Double the backoff time for the next attempt
        }
    }
        // If all barrels fail, return from cache; if not present, return an empty set.
        System.out.println("returning from cach");
        return searchInfoCache.getOrDefault(term, new ConcurrentSkipListSet<>());
    }

    // Method to queue up URLs.
    public void queueUpUrl(DepthControl url) throws RemoteException {
        //System.out.println("URL: " + url.getUrl() + " Depth: " + url.getDepth());
        if (url.getDepth() <= 2) {
            url.setTimestamp(System.currentTimeMillis());
            urlQueue.add(url);
        } else {
            //System.out.println("URL " + url.getUrl() + " not added due to depth limit");
        }
    }

    public List<String> searchURL(String url) throws RemoteException {
        int attempts = 0;
        long backoff = 3000; 
    
        while (attempts < MAX_RETRIES) {
            String barrelKey = getNextBarrelKey();
            try {
               
                BarrelInterface barrel = getBarrel(barrelKey);
                List<String> result = barrel.searchURL(url);
                searchURLCache.put(url, result);
                System.out.println(" with Barrel " + barrelKey);
                return result; // If successful, return the result immediately.
            } catch (Exception e) {
                System.err.println("Attempt " + attempts + " with Barrel " + barrelKey + " failed: " + e.toString());
                attempts++;
                try {
                    Thread.sleep(backoff + new Random().nextInt(500)); // Adding some randomization
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    throw new RemoteException("Thread interrupted during backoff", ie);
                }
                backoff *= 2; // Double the backoff time for the next attempt
            }
        }
    
        // If all retries fail, return the cached result or an empty list
        return searchURLCache.getOrDefault(url, new ArrayList<>());
    }

    @Override
    public List<String> getTopSearchedTerms() throws RemoteException {
        // Implement logic to return the top searched terms.

        // Sort the search term frequencies by value in descending order.
        List<Map.Entry<String, Integer>> sortedTerms = searchTermFrequencies.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // Extract the top 10 searched terms.
        List<String> topTerms = sortedTerms.stream()
                .limit(10)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.toList());

        return topTerms;

    }

    public Set<UUID> getActiveBarrels() throws RemoteException {
        Set<UUID> activeBarrelIds = new HashSet<>();
        for (String registryAddress : barrelRegistryAddresses) {
            try {
                String[] parts = registryAddress.split(":");
                Registry registry = LocateRegistry.getRegistry(parts[0], Integer.parseInt(parts[1]));
                BarrelInterface barrel = (BarrelInterface) registry.lookup("Barrel");
    
                UUID barrelId = barrel.getId(); // Get the Barrel's UUID
                activeBarrelIds.add(barrelId); // Add the UUID to the set if connection is successful
            } catch (Exception e) {
                System.err.println("Failed to connect to barrel at " + registryAddress + ": " + e.toString());
                // Connection failure means the barrel is not active, so its UUID isn't added
            }
        }
        return activeBarrelIds;
    }
}
