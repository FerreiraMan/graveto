package me.ferreira.graveto.common.infrastructure.http;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.common.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestRateLimiterInterceptor implements ClientHttpRequestInterceptor {

  private record RequestState(int count, Instant timestamp) {
  }

  private static final Map<UUID, RequestState> USER_REQUESTS_MAP = new ConcurrentHashMap<>();
  private final Integer maxRequests;
  private final Long requestCooldown;

  public RequestRateLimiterInterceptor(
      final @Value("${http.client.max-requests}") Integer maxRequests,
      final @Value("${http.client.request-cooldown-ms}") Long requestCooldown) {

    this.maxRequests = maxRequests;
    this.requestCooldown = requestCooldown;
  }

  // Burst window approach. Example: User can only send a certain amount of requests on a given time window.
  // After the cooldown, User will be able to send same amount of requests.
  // Current solution does not evict entries. If app is used by a large amount of users an eviction strategy should exist
  @Override
  public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                      final ClientHttpRequestExecution execution)
      throws IOException {

    final Object userSidObject = request.getAttributes().get("userSid");

    if (!(userSidObject instanceof UUID userSid)) {
      return execution.execute(request, body);
    }

    USER_REQUESTS_MAP.compute(userSid, (k, v) -> {
      final Instant now = Instant.now();
      if (v == null || now.isAfter(v.timestamp.plusMillis(requestCooldown))) {
        return new RequestState(1, now);
      }
      if (v.count < maxRequests) {
        return new RequestState(v.count + 1, now);
      }
      throw new TooManyRequestsException();
    });

    return execution.execute(request, body);
  }

}
