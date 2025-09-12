package se.bahram.ai.springboot_notion_app.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

public record BookRequest(
        @NotBlank String name,
        @NotBlank String author,
        @NotBlank String isbn,
        @PositiveOrZero Double price,
        @PastOrPresent LocalDate publishedOn,
        List<String> tags
) {}
