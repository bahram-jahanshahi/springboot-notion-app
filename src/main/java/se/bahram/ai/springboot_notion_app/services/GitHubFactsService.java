package se.bahram.ai.springboot_notion_app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import se.bahram.ai.springboot_notion_app.domain.RepoFacts;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GitHubFactsService {

    private final RestTemplate rest;
    private final ObjectMapper om;

    public GitHubFactsService(
            RestTemplateBuilder builder,
            ObjectMapper om,
            @Value("${github.token}") String token // fine-grained PAT preferred
    ) {
        this.om = om;
        this.rest = builder
                .rootUri("https://api.github.com")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .additionalInterceptors((request, body, execution) -> {
                    HttpHeaders h = request.getHeaders();
                    h.set(HttpHeaders.USER_AGENT, "TrendAgent/1.0 (+https://example.com)");
                    h.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    if (token != null && !token.isBlank()) {
                        h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Fetches all requested facts for a repo in one go.
     * @param owner repo owner
     * @param repo repo name
     * @param starsToday parsed from the Trending page via jsoup
     * @param stars30dOverride provide if you snapshot star totals daily; otherwise null
     */
    public RepoFacts fetchFacts(String owner, String repo, int starsToday, Integer stars30dOverride) {
        JsonNode repoNode = getJson("/repos/{owner}/{repo}", owner, repo);

        String description  = textOrNull(repoNode.path("description"));
        int totalStars      = repoNode.path("stargazers_count").asInt(0);
        int forks           = repoNode.path("forks_count").asInt(0);

        // Real "watching" is subscribers_count; watchers_count equals stargazers_count.
        int watching        = repoNode.hasNonNull("subscribers_count")
                ? repoNode.path("subscribers_count").asInt(0)
                : repoNode.path("watchers_count").asInt(0);

        String mainLanguage = textOrNull(repoNode.path("language"));

        // Languages (bytes per language)
        JsonNode langNode = getJson("/repos/{owner}/{repo}/languages", owner, repo);
        Map<String,Integer> langBytes = new LinkedHashMap<>();
        langNode.fields().forEachRemaining(e -> langBytes.put(e.getKey(), e.getValue().asInt(0)));

        // Other languages = keys minus main, sorted by bytes desc
        List<String> otherLanguages = langBytes.entrySet().stream()
                .filter(e -> !equalsIgnoreCaseSafe(e.getKey(), mainLanguage))
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        // Topics
        JsonNode topicsNode = getJson("/repos/{owner}/{repo}/topics", owner, repo);
        List<String> topics = new ArrayList<>();
        topicsNode.path("names").forEach(n -> topics.add(n.asText()));

        // License (SPDX)
        String licenseSpdx = repoNode.path("license").path("spdx_id").isMissingNode()
                ? null
                : textOrNull(repoNode.path("license").path("spdx_id"));

        // Contributors count (unique, can include anonymous)
        int contributors = getContributorsCount(owner, repo);

        // stars30day: prefer snapshots; else compute on-demand (rate-limit heavier)
        int stars30day = (stars30dOverride != null) ? stars30dOverride : 0;

        return new RepoFacts(
                owner,
                repo,
                description,
                totalStars,
                starsToday,
                stars30day,
                forks,
                watching,
                mainLanguage,
                otherLanguages,
                contributors,
                licenseSpdx,
                topics
        );
    }

    /**
     * Optional: compute stars in the last 30 days via the stargazer timeline.
     * Prefer snapshotting daily totals instead (cheaper & more reliable).
     */
    public int computeStars30dViaTimeline(String owner, String repo, int maxPages) {
        // We must use a different Accept to get "starred_at" timestamps.
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github.star+json");
        headers.set(HttpHeaders.USER_AGENT, "TrendAgent/1.0");
        HttpEntity<Void> req = new HttpEntity<>(headers);

        int perPage = 100;
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(30);
        int count = 0;
        int page = 1;

        try {
            while (page <= maxPages) {
                ResponseEntity<String> resp = rest.exchange(
                        "/repos/{owner}/{repo}/stargazers?per_page={perPage}&page={page}",
                        HttpMethod.GET, req, String.class, owner, repo, perPage, page);

                if (resp.getStatusCode().is4xxClientError() || resp.getStatusCode().is5xxServerError()) break;
                String body = resp.getBody();
                if (body == null || body.isBlank()) break;

                JsonNode arr = om.readTree(body);
                if (!arr.isArray() || arr.size() == 0) break;

                boolean shouldStop = false;
                for (JsonNode n : arr) {
                    // With star+json, each item has "starred_at"
                    String starredAt = textOrNull(n.path("starred_at"));
                    if (starredAt == null) continue;
                    OffsetDateTime t = OffsetDateTime.parse(starredAt);
                    if (t.isBefore(threshold)) {
                        shouldStop = true;
                        break;
                    }
                    count++;
                }

                if (shouldStop) break;

                // If less than a full page, no more pages
                if (arr.size() < perPage) break;

                page++;
            }
        } catch (RestClientResponseException | IOException e) {
            // Swallow and return best-effort; you may want to log this
            return count;
        }

        return count;
    }

    // --- helpers -------------------------------------------------------------

    private JsonNode getJson(String path, Object... uriVars) {
        ResponseEntity<String> resp = rest.getForEntity(path, String.class, uriVars);
        String body = resp.getBody();
        try {
            return (body == null || body.isBlank()) ? om.createObjectNode() : om.readTree(body);
        } catch (IOException e) {
            return om.createObjectNode();
        }
    }

    private int getContributorsCount(String owner, String repo) {
        // per_page=1 makes the "last" page number equal to the total unique contributors
        ResponseEntity<String> resp = rest.exchange(
                "/repos/{owner}/{repo}/contributors?per_page=1&anon=true",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                owner, repo
        );

        List<String> links = resp.getHeaders().get(HttpHeaders.LINK);
        String linkHeader = (links != null && !links.isEmpty()) ? links.get(0) : null;

        if (linkHeader != null && linkHeader.contains("rel=\"last\"")) {
            // ...page=123>; rel="last"
            Matcher m = Pattern.compile("page=(\\d+)>; rel=\"last\"").matcher(linkHeader);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        // If no Link header (small repos), count the items in the first (only) page
        try {
            JsonNode arr = om.readTree(resp.getBody());
            return (arr != null && arr.isArray()) ? arr.size() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static String textOrNull(JsonNode n) {
        return (n == null || n.isNull() || n.isMissingNode()) ? null : n.asText(null);
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }
}
