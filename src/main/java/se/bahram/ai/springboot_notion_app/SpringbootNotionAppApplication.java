package se.bahram.ai.springboot_notion_app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import se.bahram.ai.springboot_notion_app.domain.RepoFacts;
import se.bahram.ai.springboot_notion_app.domain.enums.GitHubTrendingDateRange;
import se.bahram.ai.springboot_notion_app.services.AddBookToNotionAppDatabaseService;
import se.bahram.ai.springboot_notion_app.services.GitHubFactsService;
import se.bahram.ai.springboot_notion_app.services.GitHubTrendingScraper;
import se.bahram.ai.springboot_notion_app.services.NotionProbeService;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class SpringbootNotionAppApplication implements CommandLineRunner {

	@Autowired
	AddBookToNotionAppDatabaseService addBookToNotionAppDatabaseService;

	@Autowired
	NotionProbeService notionProbeService;

	@Autowired
	GitHubTrendingScraper gitHubTrendingScraper;

	@Autowired
	GitHubFactsService gitHubFactsService;

	public static void main(String[] args) {
		SpringApplication.run(SpringbootNotionAppApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		//notionProbeService.getDb("26c00094-38af-80de-9770-000b907ff28c");
		/*System.out.println(
				notionProbeService.getDb("26c0009438af8070bd41faa490c5b164"));

		Map<String, Object> response = addBookToNotionAppDatabaseService.createBookPage(
				"Book from Spring Boot Application",
				"Bahram",
				"1234567890",
				29.99,
				"2023-10-01",
				List.of("Spring Boot", "Notion API", "Java")
		);
		System.out.println("Response from Notion API: " + response);*/

		gitHubTrendingScraper.fetch("", GitHubTrendingDateRange.daily, "en")
				.forEach(repo -> {
					System.out.println("Repo: " + repo);
					RepoFacts facts = gitHubFactsService.fetchFacts(repo.owner(), repo.repo(), repo.starsToday(), null);
					System.out.println("Facts: " + facts);
				});
	}
}
