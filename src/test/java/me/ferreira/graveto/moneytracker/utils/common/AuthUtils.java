package me.ferreira.graveto.moneytracker.utils.common;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public class AuthUtils {

    public static Authentication mockAuth(final UUID userSid) {
        return new UsernamePasswordAuthenticationToken(
                userSid,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

}
