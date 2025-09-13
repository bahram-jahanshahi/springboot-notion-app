package se.bahram.ai.springboot_notion_app.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddBookToNotionAppDatabaseService {

    private final RestTemplate restTemplate;
    private final String token;
    private final String databaseId;
    private final String dataSourceId;
    private final String apiVersion;

    public  AddBookToNotionAppDatabaseService(
            @Value("${notion.token}") String token,
            @Value("${notion.databaseId}") String databaseId,
            @Value("${notion.apiVersion}") String apiVersion,
            @Value("${notion.dataSourceId}") String dataSourceId
    ) {
        this.restTemplate = new RestTemplate();
        this.token = token;
        this.databaseId = databaseId;
        this.apiVersion = apiVersion;
        this.dataSourceId = dataSourceId;
    }

    public Map<String, Object> createBookPage(String name,
                                              String author,
                                              String isbn,
                                              Double price,
                                              String publishedISO,
                                              List<String> tags) {


        Map<String, Object> requestBody = Map.of(
                //"parent", Map.of("database_id", databaseId),
                "parent", Map.of( "type", "data_source_id", "data_source_id", dataSourceId),
                "properties", buildProperties(name, author, isbn, price, publishedISO, tags),
                "children", List.of(
                        Map.of("object","block","type","heading_2",
                                "heading_2", Map.of("rich_text", List.of(Map.of("text", Map.of("content","Notes"))))),
                        Map.of("object","block","type","paragraph",
                                "paragraph", Map.of("rich_text", List.of(Map.of("text", Map.of("content","First note.")))))
                )
        );


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("Notion-Version", apiVersion);


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);


        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.notion.com/v1/pages",
                entity,
                Map.class
        );


        return response.getBody();
    }

    private Map<String, Object> buildProperties(String name,
                                                String author,
                                                String isbn,
                                                Double price,
                                                String publishedISO,
                                                List<String> tags) {
        Map<String, Object> props = new LinkedHashMap<>();


        props.put("Name", Map.of(
                "title", List.of(Map.of("text", Map.of("content", name)))
        ));


        props.put("Author", Map.of(
                "rich_text", List.of(Map.of("text", Map.of("content", author)))
        ));


        props.put("ISBN", Map.of(
                "rich_text", List.of(Map.of("text", Map.of("content", isbn)))
        ));


        if (price != null) {
            props.put("Price", Map.of("number", price));
        }


        if (publishedISO != null) {
            props.put("Published", Map.of("date", Map.of("start", publishedISO)));
        }


        if (tags != null && !tags.isEmpty()) {
            List<Map<String, Object>> multi = new ArrayList<>();
            for (String t : tags) {
                multi.add(Map.of("name", t));
            }
            props.put("Tags", Map.of("multi_select", multi));
        }


        return props;
    }
}
