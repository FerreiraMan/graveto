package me.ferreira.graveto.portfolio.brokers.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;

public record AddMemberToBrokerRequestDto(
    @NotBlank(message = "Email of the new member cannot be empty")
    String email,

    @NotNull(message = "Membership role is required")
    BrokerMembershipRole role
) {
}
