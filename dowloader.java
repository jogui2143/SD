
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashSet;

public class dowloader {
    private HashSet<String> urlLinks;

    public dowloader() {
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
        dowloader obj = new dowloader();
        obj.getPageLinks("https://www.javatpoint.com/digital-electronics");
    }
}
