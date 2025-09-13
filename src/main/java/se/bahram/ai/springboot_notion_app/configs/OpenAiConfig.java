package se.bahram.ai.springboot_notion_app.configs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    /** Expose ChatClient as a bean named "chat". */
    @Bean
    public ChatClient chat(ChatClient.Builder builder) {
        // Uses defaults from application.yml (model, api key, etc.)
        return builder.build();
    }
}
