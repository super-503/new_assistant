package com.zengame.ai.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Configuration
public class NewsWebConfig {

    @Bean
    public SecurityFilterChain newsSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeRequests(rules -> rules.anyRequest().hasRole("SUPER_ADMIN"))
                .formLogin(login -> login.defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/ai/**")))
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer newsDateFormat() {
        return builder -> builder.serializers(
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
