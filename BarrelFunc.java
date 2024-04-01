import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    private ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index;

    public BarrelFunc(ConcurrentHashMap<String, ConcurrentSkipListSet<PageContent>> index) throws RemoteException {
        super();
        this.index = index;
    }

    @Override
    public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException {
        String[] words = term.toLowerCase().split("\\s+");
        ConcurrentSkipListSet<PageContent> results = new ConcurrentSkipListSet<>();

        for (String word : words) {
            if (index.containsKey(word)) {
                if (results.isEmpty()) {
                    results.addAll(index.get(word));
                } else {
                    results.retainAll(index.get(word));
                }
            } else {
                return new ConcurrentSkipListSet<>();
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
}
