import java.io.Serializable;
import java.util.List;

public class PageContent implements Serializable, Comparable<PageContent> {

    private static final int MAX_TEXT_LENGTH = 500;

    private String title;
    private String text;
    private String url;
    private List<String> references;
    private int numberOfReferences;

    public PageContent(String title, String text, String url, List<String> references, int numberOfReferences) {
        this.title = title;
        this.text = text;
        this.url = url;
        this.references = references;
        this.numberOfReferences = numberOfReferences;

        if (this.references == null) {
            this.references = List.of();
            this.numberOfReferences = 0;
        }
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getReferences() {
        return references;
    }

    public int getNumberOfReferences() {
        return numberOfReferences;
    }

    public void setNumberOfReferences(int numberOfReferences) {
        this.numberOfReferences = numberOfReferences;
    }

    public String truncateText(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(0, MAX_TEXT_LENGTH) + "...";
        }
        return text;
    }

    @Override
    public int compareTo(PageContent other) {

        return Integer.compare(other.numberOfReferences, this.numberOfReferences);
    }

    @Override
    public String toString() {
        return "PageContent{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", text='" + truncateText(text) + '\'' +
                ", numberOfReferences=" + numberOfReferences +
                '}';
    }
}