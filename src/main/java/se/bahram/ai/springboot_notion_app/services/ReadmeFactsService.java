package se.bahram.ai.springboot_notion_app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient; // adjust if your version differs
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.bahram.ai.springboot_notion_app.domain.ReadmeFacts;

import java.time.Duration;

@Service
public class ReadmeFactsService {

    private final RestTemplate rest;
    private final ChatClient chat;
    private final ObjectMapper om;

    public ReadmeFactsService(
            RestTemplateBuilder builder,
            ChatClient chat,
            ObjectMapper om,
            @Value("${github.token}") String token
    ) {
        this.chat = chat;
        this.om = om;
        this.rest = builder
                .rootUri("https://api.github.com")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .additionalInterceptors((req, body, ex) -> {
                    HttpHeaders h = req.getHeaders();
                    h.set(HttpHeaders.USER_AGENT, "TrendAgent/1.0");
                    // Raw README content:
                    // we’ll set Accept per call below to "application/vnd.github.raw"
                    if (token != null && !token.isBlank()) {
                        h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                    return ex.execute(req, body);
                })
                .build();
    }

    public ReadmeFacts extract(String owner, String repo) {
        String readme = fetchReadmeRaw(owner, repo);
        String userPrompt = """
            You are an extraction agent. Read the README.md below and return ONLY a JSON object with:
            {
              "oneLiner": string|null,
              "keyFeatures": string[]|[],
              "primaryUseCases": string[]|[],
              "installMethods": string[]|[],
              "limitations": string[]|[],
              "targetAudience": string|null
            }
            - Use ONLY info present in the README.
            - Do not add commentary. No markdown. Return valid JSON only.

            <README>
            %s
            </README>
            """.formatted(readme == null ? "" : readme);

        // Call your configured model (e.g., gpt-4o) via Spring AI
        String json = chat
                .prompt()
                .system("Extract structured facts from README and return strict JSON only.")
                .user(userPrompt)
                .call()
                .content();

        try {
            return om.readValue(json, ReadmeFacts.class);
        } catch (Exception e) {
            // If the model ever returns non-JSON, you could add a repair step; for now, fail soft.
            throw new IllegalStateException("Failed to parse ReadmeFacts JSON", e);
        }
    }

    private String fetchReadmeRaw(String owner, String repo) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github.raw"); // raw Markdown
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = rest.exchange(
                "/repos/{owner}/{repo}/readme",
                HttpMethod.GET,
                req,
                String.class,
                owner, repo
        );
        return resp.getBody();
    }
}
