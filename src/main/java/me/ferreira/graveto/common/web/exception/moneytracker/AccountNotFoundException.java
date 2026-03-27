package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(final UUID accountSid) {
        super("Account with SID [" + accountSid + "] was not found or you do not have permission to view it.");
    }

}
