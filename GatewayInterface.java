import java.rmi.*;
import java.util.HashSet;

public interface GatewayInterface extends Remote {
  public void queueUpUrl(String url) throws RemoteException;
  public HashSet<PageContent> searchinfo(String term) throws RemoteException;
  // public void searchURL(String url) throws RemoteException;
  public String getNewUrl() throws RemoteException;
}