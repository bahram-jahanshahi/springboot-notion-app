package se.bahram.ai.springboot_notion_app.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotionProbeService {

    private final RestTemplate rt = new RestTemplate();
    @Value("${notion.token}") String token;
    @Value("${notion.apiVersion}") String apiVersion;

    public Map getDb(String id) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.set("Notion-Version", apiVersion);
        HttpEntity<Void> e = new HttpEntity<>(h);

        ResponseEntity<Map> resp = rt.exchange(
                "https://api.notion.com/v1/databases/" + id,
                HttpMethod.GET, e, Map.class);

        return resp.getBody();
    }
}
