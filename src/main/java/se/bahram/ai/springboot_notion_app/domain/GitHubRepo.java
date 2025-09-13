package se.bahram.ai.springboot_notion_app.domain;

public record GitHubRepo(
        String fullName,     // "owner/repo"
        String description,  // Repo description
        String language,     // Primary programming language (if available)
        int starsToday,      // Stars gained today (parsed to int)
        String url           // Direct repo URL
) {
}
