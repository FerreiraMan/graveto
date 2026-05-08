package me.ferreira.graveto.identity.service.command;

public record RegisterCommand(
   String email,
   String password
) {}
