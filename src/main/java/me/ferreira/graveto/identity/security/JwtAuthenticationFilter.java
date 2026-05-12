package me.ferreira.graveto.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.payload.JwtPayload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(final JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                  final FilterChain filterChain) throws ServletException, IOException {

    final String authenticationHeader = request.getHeader("Authorization");

    if (authenticationHeader == null || !authenticationHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwtToken = authenticationHeader.substring("Bearer ".length());
    final JwtPayload jwt = jwtService.verifyJwtToken(jwtToken);

    final List<SimpleGrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_" + jwt.role())
    );

    final SecurityContext context = SecurityContextHolder.createEmptyContext();
    final Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(jwt.sid(), null, authorities);
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    filterChain.doFilter(request, response);
  }

}
