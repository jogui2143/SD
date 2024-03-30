import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private ConcurrentHashMap<String, HashSet<PageContent>> index;

    public BarrelFunc(ConcurrentHashMap<String, HashSet<PageContent>> index) throws RemoteException {
        super();
        this.index = index;
    }

    @Override
    public HashSet<PageContent> searchUrls(String term) throws RemoteException {
        String[] words = term.toLowerCase().split("\\s+");
        HashSet<PageContent> results = new HashSet<>();

        for (String word : words) {
            if (index.containsKey(word)) {
                if (results.isEmpty()) {
                    // For the first word, add all its PageContent objects to the results
                    results.addAll(index.get(word));
                } else {
                    // For subsequent words, retain only those PageContent objects that are also in the new set
                    results.retainAll(index.get(word));
                }
            } else {
                // If any word is not found in the index, the intersection will be empty
                return new HashSet<>();
            }
        }
        return results;
    }

    public List<String> searchURL(String url) throws RemoteException {
        for (Map.Entry<String, HashSet<PageContent>> entry : index.entrySet()) {
            for (PageContent page : entry.getValue()) {
                if (page.getUrl().equals(url)) {
                    return page.getReferences();
                }
            }
        }
        return new ArrayList<>();
    }
}
