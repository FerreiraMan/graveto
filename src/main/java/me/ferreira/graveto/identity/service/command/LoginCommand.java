package me.ferreira.graveto.identity.service.command;

public record LoginCommand(
   String email,
   String password
) {}
