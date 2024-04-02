import java.rmi.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public interface BarrelInterface extends Remote {
  public ConcurrentSkipListSet<PageContent> searchUrls(String term) throws RemoteException;
  public List<String> searchURL(String url) throws RemoteException;
  public Set<Integer> getActiveBarrels() throws RemoteException;
}