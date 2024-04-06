import java.rmi.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The <code>BarrelInterface</code> interface defines the remote methods that can be invoked on a Barrel component.
 * These methods facilitate searching for URLs and retrieving information about a Barrel instance.
 */
public interface BarrelInterface extends Remote {
    
    /**
     * Searches for URLs containing the specified term.
     * This method is used to search for URLs based on a given search term.
     * @param term The search term.
     * @return A <code>ConcurrentSkipListSet</code> containing matching <code>PageContent</code> objects.
     * @throws RemoteException If a remote communication error occurs.
     */
    public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException;
    
    /**
     * Searches for URLs containing the specified URL.
     * This method is used to search for URLs based on a given URL.
     * @param url The URL to search for.
     * @return A <code>List</code> of URLs containing the specified URL.
     * @throws RemoteException If a remote communication error occurs.
     */
    public List<String> searchURL(String url) throws RemoteException;
    
    /**
     * Retrieves the ID of the Barrel component.
     * This method is used to obtain the UUID of the Barrel component.
     * @return The UUID of the Barrel component.
     * @throws RemoteException If a remote communication error occurs.
     */
    public UUID getId() throws RemoteException;
    
    /**
     * Retrieves information about the Barrel component.
     * This method is used to obtain information about the Barrel component, such as its ID and average search time.
     * @return A <code>String</code> containing information about the Barrel component.
     * @throws RemoteException If a remote communication error occurs.
     */
    public String getBarrelInfo() throws RemoteException;
}
