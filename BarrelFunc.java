import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
        ConcurrentSkipListSet<PageContent> results = new ConcurrentSkipListSet<>();

        for (String word : words) {
            if (index.containsKey(word)) {
                if (results.isEmpty()) {
                    // For the first word, add all its PageContent objects to the results
                    results.addAll(index.get(word));
                } else {
                    // For subsequent words, retain only those PageContent objects that are also in
                    // the new set
                    results.retainAll(index.get(word));
                }
            } else {
                // If any word is not found in the index, the intersection will be empty
                return new ConcurrentSkipListSet<PageContent>();
            }
        }
        return results;
    }

    public List<String> searchURL(String url) throws RemoteException {
        for (Map.Entry<String, ConcurrentSkipListSet<PageContent>> entry : index.entrySet()) {
            for (PageContent page : entry.getValue()) {
                if (page.getUrl().equals(url)) {
                    return page.getReferences();
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public UUID getId() throws RemoteException{
        System.out.println("returnin"+this.id);
        return this.id;
    }


    
}
