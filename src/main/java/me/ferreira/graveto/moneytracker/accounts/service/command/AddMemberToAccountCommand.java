package me.ferreira.graveto.moneytracker.accounts.service.command;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;

public record AddMemberToAccountCommand(
    UUID userSid,
    UUID accountSid,
    String email,
    MembershipRole role
) {
}
