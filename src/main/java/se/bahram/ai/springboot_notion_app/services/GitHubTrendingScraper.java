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
import java.util.Optional;

@Service
public class GitHubTrendingScraper {

    public List<GitHubRepo> fetch(String language, GitHubTrendingDateRange dateRange, String spokenLanguage) throws IOException {

        var path = (language == null || language.isBlank()) ? "" : "/" + language;
        String gitHubTrendingbaseUrl = "https://github.com/trending";
        var url = gitHubTrendingbaseUrl + path + "?since=" + (dateRange == null ? "daily" : dateRange);
        url += "&spoken_language_code=" + (spokenLanguage == null ? "en" : spokenLanguage);


        System.out.println("Fetching GitHub Trending from: " + url);

        Connection jsoupConn = Jsoup.connect(url)
                .timeout(15000)
                .followRedirects(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9");
        Document doc = jsoupConn.get();


        return doc.select("article.Box-row").stream()
                .map(card -> {

                    String fullName = card.selectFirst("h2 a").attr("href").substring(1); // owner/repo
                    String desc = Optional.ofNullable(card.selectFirst("p")).map(Element::text).orElse("");
                    String lang = Optional.ofNullable(card.selectFirst("[itemprop=programmingLanguage]"))
                            .map(Element::text).orElse("");
                    String starsToday = card.select("span:matchesOwn(stars today)").text().replaceAll("\\D+","");

                    return new GitHubRepo(fullName, desc, lang, Integer.parseInt(starsToday), "https://github.com/" + fullName);
                }).toList();
    }
}
