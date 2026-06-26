package me.ferreira.graveto.common.infrastructure.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestLoggerInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                      final ClientHttpRequestExecution execution) throws IOException {
    logRequest(request, body);
    var response = execution.execute(request, body);
    logResponse(response);
    return response;
  }

  private void logRequest(final HttpRequest request, byte[] body) {
    log.info("Request: {} {}", request.getMethod(), request.getURI());
    if (body != null && body.length > 0) {
      log.info("Request body: {}", new String(body, StandardCharsets.UTF_8));
    }
  }

  private void logResponse(final ClientHttpResponse response) throws IOException {
    log.info("Response status: {}", response.getStatusCode());

    final byte[] responseBody = response.getBody().readAllBytes();
    if (responseBody.length > 0) {
      log.info("Response body: {}", new String(responseBody, StandardCharsets.UTF_8));
    }
  }

}
