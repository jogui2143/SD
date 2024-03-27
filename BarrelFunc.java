import java.io.IOException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;



public class BarrelFunc extends UnicastRemoteObject implements BarrelInterface{
  private HashMap<String, HashSet<String>> index;
  public BarrelFunc(HashMap<String, HashSet<String>> index) throws RemoteException{
    super();
    this.index = index;
  }
  @Override
  public HashSet<PageContent> searchUrls(String term) throws RemoteException {
      String[] words = term.toLowerCase().split("\\s+");
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
  
      HashSet<PageContent> pageContents = new HashSet<>();
      for (String url : urlsResult) {
          PageContent content = fetchPageContent(url);
          if (content != null) {
              pageContents.add(content);
          }
      }
  
      return pageContents;
  }
  
 private PageContent fetchPageContent(String url) {
    try {
        // Connect to the URL and parse the HTML document.
        Document doc = Jsoup.connect(url).get();

        // Extract the title and text content of the web page.
        String title = doc.title();
        String text = doc.body().text();

        return new PageContent(title, text, url);
    } catch (IOException e) {
        System.err.println("Error fetching page content for URL " + url + ": " + e.getMessage());
        // In case of an error, return a PageContent with the URL and empty title and text.
        return new PageContent("", "", url);
    }
}

}
  