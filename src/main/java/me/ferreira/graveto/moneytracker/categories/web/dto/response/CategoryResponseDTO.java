package me.ferreira.graveto.moneytracker.categories.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryResponseDTO(
    UUID sid,
    String name,
    UUID parentSid,
    boolean isSystem
) {}
