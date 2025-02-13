package dev.brianmiller.music.releases.pauseandplay;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import com.myalbumdj.restclient.pauseandplay.data.Release;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 *
 */
public class ParseWeeklyReleasesPage {

    private static HttpClient httpClient;

    static {
        httpClient = HttpClient.newHttpClient();
    }

    /**
     *
     */
    public static void main(String[] args) {

        try {
            Scanner scanner = new Scanner(
                    Path.of("release_pages.txt")); // change to fully qualified absolute path as needed

            List<Release> allReleases = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String pageUrl = scanner.nextLine();
                List<Release> releases = getReleasesForWeek(pageUrl);

                allReleases.addAll(releases);
            }
            allReleases.sort(Comparator.comparing(Release::artist));

            for (int i = 0; i < allReleases.size(); i++) {
                Release release = allReleases.get(i);
                System.out.printf("%02d. %s: %s (label: \"%s\") (due date: \"%s\")\n", i+1, release.artist(),
                        release.title(), release.label(), release.dueDate());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */
    public static List<Release> getReleasesForWeek(String releasesPageURL) {

        List<Release> releasesList = new ArrayList<>();

        try {
            URL url = new URL(releasesPageURL);
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(url.toURI())
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                System.err.println("Status code: " + response.statusCode());
            } else {
                String body = response.body();

                Document doc = Jsoup.parse(body);
                Element content = doc.getElementById("content");
                String[] headers = new String[] { "h2", "h3" };
                for (String header : headers) {
                    Elements h3Headers = content.getElementsByTag(header);
                    for (var h3 : h3Headers) {
                        String h3Text = h3.text();
                        if ("New stuff".equals(h3Text)) {
                            var nextElementSibling = h3.nextElementSibling();
                            if (nextElementSibling != null) {
                                String nextTagName = nextElementSibling.tagName();
                                if ("table".equals(nextTagName)) {
                                    var rows = nextElementSibling.getElementsByTag("tr");
                                    for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
                                        var rowData = rows.get(rowNum);
                                        var tdElements = rowData.getElementsByTag("td");
                                        String artist = tdElements.get(0).text();
                                        String title = tdElements.get(1).text();
                                        String thirdColumn = tdElements.get(2).text();
                                        int firstCommaInLabelDueDate = thirdColumn.indexOf(",");
                                        String label;
                                        String dueDate;
                                        if (firstCommaInLabelDueDate > -1) {
                                            label = thirdColumn.substring(1, firstCommaInLabelDueDate);
                                            dueDate = thirdColumn.substring(firstCommaInLabelDueDate + 2, thirdColumn.length() - 1);
                                        } else {
                                            label = thirdColumn;
                                            int lastSlashInURL = releasesPageURL.lastIndexOf("/");
                                            if (lastSlashInURL == -1) {
                                                dueDate = "UNKNOWN";
                                            } else {
                                                dueDate = releasesPageURL.substring(lastSlashInURL + 1);
                                            }
                                        }

                                        Release release = new Release(artist, title, label, dueDate);
                                        releasesList.add(release);
                                    }
                                }
                            }
                        }

                    }
                }
            }

        } catch (IOException | URISyntaxException | InterruptedException ex) {
            ex.printStackTrace(System.err);
        }

        return releasesList;
    }
}
