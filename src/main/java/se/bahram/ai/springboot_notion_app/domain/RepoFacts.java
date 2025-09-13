package se.bahram.ai.springboot_notion_app.domain;

import java.util.List;

/**
 * Immutable facts collected for a GitHub repository.
 */
public record RepoFacts(
        String owner,                 // repo owner (e.g., "openai")
        String repo,                  // repo name (e.g., "whisper")
        String description,           // repo description
        int totalStars,               // stargazers_count
        int starsToday,               // parsed from Trending
        int stars30day,               // computed via snapshots or timeline
        int forks,                    // forks_count
        int watching,                 // subscribers_count (true watchers)
        String mainLanguage,          // primary language (from /repos)
        List<String> otherLanguages,  // other languages (from /languages), sorted by bytes desc
        int contributors,             // unique contributors (via Link rel="last" trick)
        String licenseSpdx,           // SPDX id (e.g., "MIT", "Apache-2.0")
        List<String> topics           // repo topics (from /topics)
) {}
