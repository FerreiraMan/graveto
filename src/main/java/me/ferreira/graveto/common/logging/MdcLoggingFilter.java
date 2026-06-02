package me.ferreira.graveto.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

  private static final String TRACE_ID_KEY = "traceId";
  private static final String USER_SID_KEY = "userSid";

  @Override
  protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                  final FilterChain filterChain) throws ServletException, IOException {

    try {
      MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());

      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof UUID userSid) {
        MDC.put(USER_SID_KEY, userSid.toString());
      }

      log.info("Received request: {} - {}", request.getMethod(), request.getRequestURI());
      filterChain.doFilter(request, response);

    } finally {
      log.info("Completed request: {} - {}", request.getMethod(), request.getRequestURI());
      MDC.clear();
    }
  }

}
