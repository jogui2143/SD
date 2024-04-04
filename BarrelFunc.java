import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private UUID id; // Unique ID for this BarrelFunc instance
    private HashMap<String, ConcurrentSkipListSet<PageContent>> index;
    private List<Long> searchTimes = Collections.synchronizedList(new ArrayList<>()); // Thread-safe list to store search times

    // Constructor with ID parameter
    public BarrelFunc(UUID id, HashMap<String, ConcurrentSkipListSet<PageContent>> index) throws RemoteException {
        super();
        this.id = id; // Initialize the ID
        this.index = index;
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

public List<String> searchURL(String url) throws RemoteException {
    long startTime = System.nanoTime();
    List<String> result = index.values().parallelStream()
                               .flatMap(Collection::stream)
                               .filter(page -> page.getReferences().contains(url))
                               .map(PageContent::getUrl)
                               .distinct()
                               .collect(Collectors.toList());
    long endTime = System.nanoTime();
    searchTimes.add((endTime - startTime) / 100_000_000);

   
    return result;
}



    @Override
    public String getBarrelInfo() throws RemoteException {
        double averageTime;
        synchronized (searchTimes) {
            averageTime = searchTimes.isEmpty() ? 0.0 : 
                searchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        return "ID: " + this.id.toString() + ", Avg Time: " + averageTime + " deciseconds";
    }

    @Override
    public UUID getId() throws RemoteException{
        System.out.println("Returning ID: " + this.id);
        return this.id;
    }
}
