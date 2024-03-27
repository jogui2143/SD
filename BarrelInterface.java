import java.rmi.*;
import java.util.*;

public interface BarrelInterface extends Remote {
  public HashSet<PageContent> searchUrls(String term) throws RemoteException;
}