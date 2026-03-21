package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.util.UUID;

public record MembershipResponseDTO(
   UUID sid,
   String role
) {}
