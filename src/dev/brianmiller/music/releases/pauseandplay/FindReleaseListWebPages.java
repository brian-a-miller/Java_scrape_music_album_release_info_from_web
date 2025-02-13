package dev.brianmiller.music.releases.pauseandplay;

import dev.brianmiller.music.releases.data.Release;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;

public class FindReleaseListWebPages {

    private static HttpClient httpClient;
    private static List<String> pastYears;

    static {
        httpClient = HttpClient.newHttpClient();

        pastYears = new ArrayList<>();
        pastYears.add("https://www.pauseandplay.com/release-dates/past-2011-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2012-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2013-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2014-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2015-releases-2/");
        pastYears.add("https://www.pauseandplay.com/past-2016-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2017-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2018-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2019-releases/");
        pastYears.add("https://www.pauseandplay.com/past-2020-releases/");
    }

    /**
     *
     */
    public static void main(String[] args) {

        List<String> listOfWebPages = getReleasePageLinks();

        List<Release> allReleases = new ArrayList<>();

        for (String url : listOfWebPages) {
            List<Release> releases = ParseWeeklyReleasesPage.getReleasesForWeek(url + "/");
            allReleases.addAll(releases);
        }

        allReleases.sort(Comparator.comparing(Release::artist));
        for (int i = 0; i < allReleases.size(); i++) {
            Release release = allReleases.get(i);
            System.out.printf("%02d. %s: %s (label: \"%s\") (due date: \"%s\")\n", i+1, release.artist(), release.title(), release.label(), release.dueDate());
        }
    }

    /**
     *
     */
    public static List<String> getReleasePageLinks() {

        List<String> results = new ArrayList<>();

        for (String yearSummary : pastYears) {

            try {
                URL url = new URL(yearSummary);
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
                    String result = response.body();
                    String A_TAG_MATCHING_GROUP = "<a href=([\"'])(https://www.pauseandplay.com/[adfjmnos].*-20[12][0-9])/\\1";

                    Matcher matcher = Pattern.compile(A_TAG_MATCHING_GROUP).matcher(result);
                    while (matcher.find()) {

                        results.add(matcher.group(2));
                    }
                }

            } catch (IOException | URISyntaxException | InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }

        Collections.sort(results);

        return results;
    }
}
