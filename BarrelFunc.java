import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface{
  private HashMap<String, HashSet<String>> index;
  public BarrelFunc(HashMap<String, HashSet<String>> index) throws RemoteException{
    super();
    this.index = index;
  }

  @Override
  public HashSet<String> searchUrls(String term) throws RemoteException{
    String[] words = term.toLowerCase().split(" \\s+");
    HashSet<String> urlsResult = new HashSet<>();

    if(words.length > 0 && index.containsKey(words[0])){
      urlsResult.addAll(index.get(words[0]));
    } else {
      return new HashSet<>();
    }

    for(int i = 1; i < words.length; i++){
      if(index.containsKey(words[i])){
        urlsResult.retainAll(index.get(words[i]));
      } else {

        urlsResult.clear();
        break;
      }
    }
    return urlsResult;
  }
}