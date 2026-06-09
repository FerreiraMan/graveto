package me.ferreira.graveto.moneytracker.accounts.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;

public record AddMemberToAccountRequestDto(
    @NotBlank(message = "Email of the new member cannot be empty")
    String email,

    @NotNull(message = "Membership role is required")
    MembershipRole role
) {
}
