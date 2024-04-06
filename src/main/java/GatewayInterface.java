import java.rmi.*;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;


/**
 * Remote interface for the gateway component in a distributed web crawling system.
 * This interface defines methods for queuing up URLs, retrieving new URLs, searching URLs, retrieving search results,
 * obtaining top searched terms, and getting active barrels from the gateway.
 */
public interface GatewayInterface extends Remote {

  /**
   * Queues up a new URL for crawling.
   * @param url The DepthControl object representing the URL and its depth.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public void queueUpUrl(DepthControl url) throws RemoteException;

  /**
   * Retrieves a new URL to be crawled.
   * @return The DepthControl object representing the new URL and its depth.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public DepthControl getNewUrl() throws RemoteException;

  /**
   * Searches for URLs that match the specified URL.
   * @param url The URL to search for.
   * @return A list of URLs matching the specified URL.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public List<String> searchURL(String url) throws RemoteException;

  /**
   * Searches for pages containing the specified term.
   * @param term The term to search for.
   * @return A set of page contents containing the specified term.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public ConcurrentSkipListSet<PageContent> searchInfo(String term) throws RemoteException;

  /**
   * Retrieves the top searched terms.
   * @return A list of top searched terms.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public List<String> getTopSearchedTerms() throws RemoteException;

  /**
   * Retrieves the set of active barrels.
   * @return A set of active barrels.
   * @throws RemoteException if there is an issue with the remote method invocation.
   */
  public Set<String> getActiveBarrels() throws RemoteException;
}