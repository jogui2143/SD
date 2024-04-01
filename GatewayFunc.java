// Importing necessary Java RMI and concurrency classes.
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

// GatewayFunc class declaration, extending UnicastRemoteObject to enable RMI, and implementing GatewayInterface.
public class GatewayFunc extends UnicastRemoteObject implements GatewayInterface {
    // A concurrent queue to store URLs. ConcurrentLinkedQueue ensures thread-safe operations.
    private ConcurrentLinkedQueue<DepthControl> urlQueue = new ConcurrentLinkedQueue<>();
    // Comment: "Queue of URLs, I guess :)" (presumably a note from the developer).

    private final ConcurrentHashMap<String, Integer> searchTermFrequencies = new ConcurrentHashMap<>();



    // Constructor for GatewayFunc.
    public GatewayFunc() throws RemoteException {
        super(); // Calling the constructor of UnicastRemoteObject.
    }

    // Increment the count for a search term.
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
    public HashSet<PageContent> searchinfo(String term) throws RemoteException {
        try {
            Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
            BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
           //System.out.println("no search info ");
            recordSearchTerm(term);
            return barrel.searchUrls(term);
        } catch (Exception e) {
            System.err.println("Exception on searchinfo(didn't connect to the Barrel)" + e.toString());
            throw new RemoteException("Exception on searchinfo(didn't connect to the Barrel)" + e.toString());
        }
    }

    // Method to queue up URLs.
    public void queueUpUrl(DepthControl url) throws RemoteException {
        // Adds a new URL to the concurrent queue.
        System.out.println("URL: " + url.getUrl() + " Depth: " + url.getDepth());
        if(url.getDepth() > 2){
            return;
        }
        urlQueue.add(url);
    }

    public List<String> searchURL(String url) throws RemoteException {
        try {
            Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
            BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
            return barrel.searchURL(url);
        } catch (Exception e) {
            System.err.println("Exception on searchURL(didn't connect to the Barrel)" + e.toString());
            throw new RemoteException("Exception on searchURL(didn't connect to the Barrel)" + e.toString());
        }
    }

    @Override
    public List<String> getTopSearchedTerms() {
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

    /*
     * @Override
     * public long getServerResponseTime() {
     * // Implement logic to return the server response time.
     * }
     */

}
