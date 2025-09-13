package se.bahram.ai.springboot_notion_app.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Structured facts extracted from a repository's README.md.
 * Designed for direct JSON deserialization from an LLM response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReadmeFacts(
        String oneLiner,
        List<String> keyFeatures,
        List<String> primaryUseCases,
        List<String> installMethods,
        List<String> limitations,
        String targetAudience
) {}
