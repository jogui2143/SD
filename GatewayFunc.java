import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GatewayFunc extends UnicastRemoteObject implements GatewayInterface{
  private ConcurrentLinkedQueue<String> urlQueue = new ConcurrentLinkedQueue<>();
  //fila de Urls I guess :)

  public GatewayFunc() throws RemoteException{
    super();
  }

  @Override
  public String getNewUrl() throws RemoteException{
    return urlQueue.poll();
  }

  @Override
  public HashSet<String> searchinfo(String term) throws RemoteException{
    try{
      Registry reg2 = LocateRegistry.getRegistry("localhost", 1099);
      BarrelInterface barrel = (BarrelInterface) reg2.lookup("Barrel");
      return barrel.searchUrls(term);
    } catch (Exception e){
      System.err.println("Exception on searchinfo(não conectou a merda do barrel)" + e.toString());
      throw new RemoteException("Exception on searchinfo(não conectou a merda do barrel)" + e.toString());
    }
  }

  public void queueUpUrl(String url) throws RemoteException{
    urlQueue.add(url);
  }


}