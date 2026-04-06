package com.fpsweeper.harvest.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    private OAuth2AuthenticationSuccessHandler customOAuth2SuccessHandler;

    @Autowired
    private CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                                },
                                request ->
                                        request.getRequestURI().startsWith("/api/")
                                                || request.getRequestURI().startsWith("/auth/")
                        )
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Public endpoints ───────────────────────────────────────────
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/hello",
                                "/auth/login",
                                "/auth/register",
                                "/auth/verify",
                                "/auth/google",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/api/solana/health",
                                "/api/chains/supported",
                                "/api/chains/pricing"
                        ).permitAll()
                        .requestMatchers("/api/market-data/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()

                        // ✅ Public — landing page fetches packages without auth
                        .requestMatchers(HttpMethod.GET, "/api/points/packages").permitAll()

                        // ── Protected bot endpoints ────────────────────────────────────
                        .requestMatchers("/api/bots/**").authenticated()
                        .requestMatchers("/api/bot-execution/**").authenticated()

                        // ── Other protected endpoints ──────────────────────────────────
                        .requestMatchers(
                                "/auth/me",
                                "/api/solana/**",
                                "/api/social/**",
                                "/api/points/**",
                                "/api/deposits/**",
                                "/api/chains/calculate",
                                "/api/admin/**",
                                "/api/wallet/**",
                                "/api/pending-deposits/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(discordOAuth2UserService())
                        )
                        .successHandler(customOAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            System.err.println("===== OAuth2 Failure =====");
                            System.err.println("Error: " + exception.getMessage());
                            exception.printStackTrace();
                            response.sendRedirect(frontendUrl + "/profile?discord=error");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://harvest3.com",
                "https://www.harvest3.com",
                "https://api.harvest3.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> discordOAuth2UserService() {
        return userRequest -> {
            System.out.println("\n===== DISCORD OAUTH USER SERVICE =====");

            String accessToken = userRequest.getAccessToken().getTokenValue();
            System.out.println("Access Token: " + accessToken.substring(0, 20) + "...");

            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://discord.com/api/users/@me";

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                System.out.println("Fetching from: " + url);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                System.out.println("Response Status: " + response.getStatusCode());
                System.out.println("Raw Response Body:");
                System.out.println(response.getBody());

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(response.getBody(), Map.class);

                System.out.println("Parsed Response: " + data);

                String id = String.valueOf(data.get("id"));
                String username = (String) data.get("username");
                String discriminator = (String) data.get("discriminator");
                String avatar = (String) data.get("avatar");
                String globalName = (String) data.get("global_name");

                System.out.println("Extracted:");
                System.out.println("  ID: " + id);
                System.out.println("  Username: " + username);
                System.out.println("  Discriminator: " + discriminator);
                System.out.println("  Global Name: " + globalName);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("id", id);
                attributes.put("username", username);
                attributes.put("discriminator", discriminator);
                attributes.put("global_name", globalName);
                attributes.put("avatar", avatar);

                System.out.println("Final attributes: " + attributes);

                OAuth2User oauth2User = new DefaultOAuth2User(
                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                        attributes,
                        "id"
                );

                System.out.println("Successfully created OAuth2User");
                System.out.println("===== DISCORD OAUTH USER SERVICE END =====\n");

                return oauth2User;

            } catch (Exception e) {
                System.err.println("\n===== DISCORD OAUTH ERROR =====");
                System.err.println("Exception: " + e.getClass().getName());
                System.err.println("Message: " + e.getMessage());
                e.printStackTrace();
                System.err.println("===== ERROR END =====\n");
                throw new RuntimeException("Failed to fetch Discord user info", e);
            }
        };
    }
}