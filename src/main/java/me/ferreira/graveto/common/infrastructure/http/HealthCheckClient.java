package me.ferreira.graveto.common.infrastructure.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HealthCheckClient {

  public static void main(final String[] args) throws InterruptedException, IOException {
    if (args.length == 0) {
      System.out.println("Please append the App's port. Example: java HealthCheckClient.java 8081");
      throw new RuntimeException("Argument port missing");
    }

    var request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + args[0] + "/actuator/health"))
        .header("accept", "application/json")
        .build();

    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200 || !response.body().contains("UP")) {
      throw new RuntimeException("Healthcheck failed.");
    }
    System.out.println("Healthcheck succeeded.");
  }

}
