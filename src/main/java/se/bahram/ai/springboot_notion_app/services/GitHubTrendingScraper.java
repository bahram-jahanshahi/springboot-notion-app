package se.bahram.ai.springboot_notion_app.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import se.bahram.ai.springboot_notion_app.domain.GitHubRepo;
import se.bahram.ai.springboot_notion_app.domain.enums.GitHubTrendingDateRange;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class GitHubTrendingScraper {

    private static final Pattern OWNER_REPO = Pattern.compile("^/([^/]+)/([^/]+)$");

    public List<GitHubRepo> fetch(String language, GitHubTrendingDateRange dateRange, String spokenLanguage) throws IOException {

        String path = (language == null || language.isBlank()) ? "" : "/" + language;
        String baseUrl = "https://github.com/trending";
        String since = (dateRange == null) ? "daily" : dateRange.name().toLowerCase();
        String spoken = (spokenLanguage == null || spokenLanguage.isBlank()) ? "en" : spokenLanguage;

        String url = baseUrl + path + "?since=" + since + "&spoken_language_code=" + spoken;
        System.out.println("Fetching GitHub Trending from: " + url);

        Connection jsoupConn = Jsoup.connect(url)
                .timeout(15000)
                .followRedirects(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9");

        Document doc = jsoupConn.get();

        return doc.select("article.Box-row").stream()
                // ensure it's a regular repo card like /owner/repo
                .map(card -> {
                    Element a = card.selectFirst("h2 a[href]");
                    if (a == null) return null;

                    String href = a.attr("href"); // e.g. /owner/repo
                    var m = OWNER_REPO.matcher(href);
                    if (!m.matches()) return null;

                    String owner = m.group(1);
                    String repo  = m.group(2);

                    String desc = Optional.ofNullable(card.selectFirst("p"))
                            .map(Element::text).orElse("");

                    String lang = Optional.ofNullable(card.selectFirst("[itemprop=programmingLanguage]"))
                            .map(Element::text).orElse("");

                    // Prefer language-agnostic spot for daily stars.
                    // If not present, fall back to the English phrase.
                    int starsToday = Optional.ofNullable(card.selectFirst("span.float-sm-right"))
                            .map(Element::text)
                            .map(t -> t.replaceAll("\\D+", ""))
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .orElseGet(() -> safeParseInt(
                                    card.select("span:matchesOwn(stars today)")
                                            .text().replaceAll("\\D+", ""))
                            );

                    String repoUrl = "https://github.com/" + owner + "/" + repo;

                    // ⬇️ Adjust this constructor to your actual GitHubRepo signature.
                    // If your GitHubRepo still expects fullName, build it with owner + "/" + repo.
                    return new GitHubRepo(
                            owner,
                            repo,
                            desc,
                            lang,
                            starsToday,
                            repoUrl
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static int safeParseInt(String s) {
        try {
            return (s == null || s.isBlank()) ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
