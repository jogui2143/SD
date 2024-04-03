import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private UUID id; // Unique ID for this BarrelFunc instance
    private ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index;
   
    // Constructor with ID parameter
    public BarrelFunc(UUID id, ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index) throws RemoteException {
        super();
        this.id = id; // Initialize the ID
        this.index = index;
    }

    @Override
public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException {
    String[] words = term.toLowerCase().split("\\s+");
    
    // Create a list of sets along with word length
    List<ConcurrentSkipListSet<PageContent>> listOfSets = new ArrayList<>();
    for (String word : words) {
        if (index.containsKey(word)) {
            listOfSets.add(index.get(word));
        } else {
            // If any word is not found, no need to continue
            return new ConcurrentSkipListSet<>();
        }
    }

    // Sort the list based on the size of each set
    listOfSets.sort(Comparator.comparingInt(Set::size));

    // Start with the smallest set
    Iterator<ConcurrentSkipListSet<PageContent>> it = listOfSets.iterator();
    ConcurrentSkipListSet<PageContent> results = new ConcurrentSkipListSet<>(it.next());

    // Intersect with the rest
    while (it.hasNext()) {
        results.retainAll(it.next());
        if (results.isEmpty()) {
            // If intersection is empty, return immediately
            return results;
        }
    }

    return results;
}
    public List<String> searchURL(String url) throws RemoteException {
    return index.values().parallelStream()
                 .flatMap(Collection::stream)
                 .filter(page -> page.getReferences().contains(url))
                 .map(PageContent::getUrl)
                 .distinct()
                 .collect(Collectors.toList());
}

    @Override
    public UUID getId() throws RemoteException{
        System.out.println("returnin"+this.id);
        return this.id;
    }


    
}
