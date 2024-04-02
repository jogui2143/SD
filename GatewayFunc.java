
// Importing necessary Java RMI and concurrency classes.
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public GatewayFunc() throws RemoteException {
        super(); // Calling the constructor of UnicastRemoteObject.
    }

    public void recordSearchTerm(String term) {
        searchTermFrequencies.merge(term, 1, Integer::sum);
    }

    // Overriding the getNewUrl method from GatewayInterface.
    @Override
    public DepthControl getNewUrl() throws RemoteException {
        // Polls and returns the head of the URL queue, or null if the queue is empty.
        System.out.println("" + urlQueue.size() + " URLs in the queue");
        return urlQueue.poll();
    }

    // Overriding the searchinfo method from GatewayInterface.
    @Override
    public ConcurrentSkipListSet<PageContent> searchinfo(String term) throws RemoteException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
                BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
                recordSearchTerm(term);
                ConcurrentSkipListSet<PageContent> result = barrel.searchUrls(term);
                searchInfoCache.put(term, new ConcurrentSkipListSet<>(result));
                return result;
            } catch (Exception e) {
                attempts++;
                System.err.println("Attempt " + attempts + " failed: " + e.toString());
            }
        }
        // Return from cache; if not present, return an empty set.
        return searchInfoCache.getOrDefault(term, new ConcurrentSkipListSet<>());
    }
    // Method to queue up URLs.
    public void queueUpUrl(DepthControl url) throws RemoteException {
        System.out.println("URL: " + url.getUrl() + " Depth: " + url.getDepth());
        if (url.getDepth() <= 2) {
            url.setTimestamp(System.currentTimeMillis());
            urlQueue.add(url);
        } else {
            System.out.println("URL " + url.getUrl() + " not added due to depth limit");
        }
    }

    public List<String> searchURL(String url) throws RemoteException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
                BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
                List<String> result = barrel.searchURL(url);
                searchURLCache.put(url, result);
                return result;
            } catch (Exception e) {
                attempts++;
                System.err.println("Attempt " + attempts + " failed: " + e.toString());
            }
        }
        return searchURLCache.getOrDefault(url, new ArrayList<>());
    }

    @Override
    public List<String> getTopSearchedTerms() throws RemoteException{
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

    public Set<Integer> getActiveBarrels() throws RemoteException {
        try {
            Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
            BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
            return barrel.getActiveBarrels();
        } catch (Exception e) {
            System.err.println("Num deu" + e.toString());
            throw new RemoteException("Num deu" + e.toString());
        }
    }
}
