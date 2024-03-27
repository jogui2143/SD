import java.io.IOException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface {
    // Updated to store PageContent objects instead of just URLs.
    private HashMap<String, HashSet<PageContent>> index;

    public BarrelFunc(HashMap<String, HashSet<PageContent>> index) throws RemoteException {
        super();
        this.index = index;
    }

    @Override
    public HashSet<PageContent> searchUrls(String term) throws RemoteException {
        String[] words = term.toLowerCase().split("\\s+");
        HashSet<PageContent> pagesResult = new HashSet<>();

        // Check if there are words to search for.
        if (words.length > 0 && index.containsKey(words[0])) {
            pagesResult.addAll(index.get(words[0]));
        } else {
            return new HashSet<>();
        }

        // Intersect the sets of PageContent objects for each word.
        for (int i = 1; i < words.length; i++) {
            if (index.containsKey(words[i])) {
                pagesResult.retainAll(index.get(words[i]));
            } else {
                // If any word is not found, return empty set.
                return new HashSet<>();
            }
        }

        return pagesResult;  // Return the set of PageContent objects.
    }
}
