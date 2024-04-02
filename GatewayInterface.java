import java.rmi.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public interface GatewayInterface extends Remote {
  public void queueUpUrl(DepthControl url) throws RemoteException;
  // public void searchURL(String url) throws RemoteException;
  public DepthControl getNewUrl() throws RemoteException;
  public List<String> searchURL(String url) throws RemoteException;
  public ConcurrentSkipListSet<PageContent> searchinfo(String term) throws RemoteException;
  public List<String> getTopSearchedTerms() throws RemoteException;
}