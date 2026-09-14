package me.ferreira.graveto.identity.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import me.ferreira.graveto.common.config.properties.HttpProperties;
import me.ferreira.graveto.common.logging.MdcLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  private final HttpProperties httpProperties;
  private final JwtAuthenticationFilter jwtAuthFilter;
  private final ExceptionHandlerFilter exceptionHandlerFilter;

  public SecurityConfiguration(final HttpProperties httpProperties, final JwtAuthenticationFilter filter,
                               final ExceptionHandlerFilter exceptionHandlerFilter) {
    this.httpProperties = httpProperties;
    this.jwtAuthFilter = filter;
    this.exceptionHandlerFilter = exceptionHandlerFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {

    http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(exceptionHandlerFilter, LogoutFilter.class)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new MdcLoggingFilter(), JwtAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**", "/error", "/actuator/**").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {

    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(httpProperties.cors().allowedOrigins());
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    configuration.setMaxAge(Duration.ofMinutes(10));
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(final AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
