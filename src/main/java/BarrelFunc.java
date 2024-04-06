import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private UUID id;
    private ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index;
    private ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex; // New index for hyperlinks
    private List<Long> searchTimes = Collections.synchronizedList(new ArrayList<>());

    public BarrelFunc(UUID id, ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> pages,
            ConcurrentHashMap<String, Set<String>> urlHyperlinkIndex2) throws RemoteException {
        super();
        this.id = id;
        this.index = pages;
        this.urlHyperlinkIndex = urlHyperlinkIndex2; // Initialize the new index
        
    }

    @Override
    public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException {
        long startTime = System.nanoTime();
        String[] words = term.toLowerCase().split("\\s+");

        List<ConcurrentSkipListSet<PageContent>> listOfSets = new ArrayList<>();
        for (String word : words) {
            if (index.containsKey(word)) {
                listOfSets.add(index.get(word));
            } else {
                return new ConcurrentSkipListSet<>();
            }
        }

        // Remove the sorting step since the sets are already sorted in the Barrel class

        Iterator<ConcurrentSkipListSet<PageContent>> it = listOfSets.iterator();
        ConcurrentSkipListSet<PageContent> results = new ConcurrentSkipListSet<>(it.next());

        while (it.hasNext()) {
            results.retainAll(it.next());
            if (results.isEmpty()) {
                return results;
            }
        }
        long endTime = System.nanoTime();
        searchTimes.add((endTime - startTime) / 100000);
        return results;
    }

    public List<String> searchURL(String hyperlink) throws RemoteException {
        long startTime = System.nanoTime();
        List<String> result = new ArrayList<>();
    
        // Iterate over the urlHyperlinkIndex to print all URLs and their hyperlinks
        for (Map.Entry<String, Set<String>> entry : urlHyperlinkIndex.entrySet()) {
           
    
            // Check if this URL contains the queried hyperlink and add it to the result if it does
            if (entry.getValue().contains(hyperlink)) {
                result.add(entry.getKey());
            }
        }
    
        long endTime = System.nanoTime();
        searchTimes.add((endTime - startTime) / 100_000_000); // Record the search time
        return result;
    }

    @Override
    public String getBarrelInfo() throws RemoteException {
        double averageTime;
        synchronized (searchTimes) {
            averageTime = searchTimes.isEmpty() ? 0.0
                    : searchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        return "ID: " + this.id.toString() + ", Avg Time: " + averageTime + " deciseconds";
    }

    @Override
    public UUID getId() throws RemoteException {
        System.out.println("Returning ID: " + this.id);
        return this.id;
    }
}
