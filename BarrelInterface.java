import java.rmi.*;
import java.util.*;

public interface BarrelInterface extends Remote {
  public HashSet<String> searchUrls(String term) throws RemoteException;
}