
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

    public void getPageLinks(String URL) {
        if (!urlLinks.contains(URL)) {
            try {
                if (urlLinks.add(URL)) {
                    System.out.println(URL);
                }

                Document document = Jsoup.connect(URL).get();
                Elements linksOnPage = document.select("a[href]");

                for (Element page : linksOnPage) {
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
