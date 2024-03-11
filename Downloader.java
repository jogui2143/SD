
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashSet;

public class downloader {
    private HashSet<String> urlLinks;

    public downloader() {
        urlLinks = new HashSet<String>();
    }

    // create getPageLink() method that finds all the page link in the given URL  
    public void getPageLinks(String URL) {
        // we use the conditional statement to check whether we have already crawled the URL or not.
        if (!urlLinks.contains(URL)) {
            try {
                 // if the URL is not present in the set, we add it to the set  
                if (urlLinks.add(URL)) {
                    System.out.println(URL);
                }
                // fetch the HTML code of the given URL by using the connect() and get() method and store the result in Documen
                Document document = Jsoup.connect(URL).get();
                // we use the select() method to parse the HTML code for extracting links of other URLs and store them into Elements
                Elements linksOnPage = document.select("a[href]");
 // for each extracted URL, we repeat process  
                for (Element page : linksOnPage) {
                    // call getPageLinks() method and pass the extracted URL to it as an argument
                    getPageLinks(page.attr("abs:href"));
                }
            } catch (IOException e) {
                System.err.println("For '" + URL + "': " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        downloader obj = new downloader();
        obj.getPageLinks("https://www.javatpoint.com/digital-electronics");
    }
}
