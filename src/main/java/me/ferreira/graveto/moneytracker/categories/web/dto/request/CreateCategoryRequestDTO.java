package me.ferreira.graveto.moneytracker.categories.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCategoryRequestDTO(
    @NotBlank(message = "Category name cannot be empty")
    String name,
    UUID parentSid
) {}
