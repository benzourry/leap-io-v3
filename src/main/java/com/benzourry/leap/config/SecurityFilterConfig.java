package com.benzourry.leap.config;

//import com.benzourry.leap.CustomRequestEntityConverter;

import com.benzourry.leap.security.*;
import com.benzourry.leap.security.oauth2.CustomOAuth2UserService;
import com.benzourry.leap.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.benzourry.leap.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.benzourry.leap.security.oauth2.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityFilterConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final ApiKeyAuthFilter authFilter;

    private final ImpersonationFilter impersonationFilter;

    public SecurityFilterConfig(CustomUserDetailsService customUserDetailsService,
                                CustomOAuth2UserService customOAuth2UserService,
                                ClientRegistrationRepository clientRegistrationRepository,
                                OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                                ApiKeyAuthFilter authFilter,
                                ImpersonationFilter impersonationFilter,
                                HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.authFilter = authFilter;
        this.impersonationFilter = impersonationFilter;
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(customUserDetailsService);
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        CustomAuthorizationRequestResolver resolver = new CustomAuthorizationRequestResolver(this.clientRegistrationRepository);
        HttpCookieOAuth2AuthorizationRequestRepository cookieRepo = cookieAuthorizationRequestRepository();

        // --- NEW: Lightweight custom filter for SarawakID POST logic ---
        OncePerRequestFilter sarawakIdPostFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                // If the user is trying to log in specifically with SarawakID
                if (request.getRequestURI().endsWith("/sarawakid")) {
                    OAuth2AuthorizationRequest authRequest = resolver.resolve(request);
                    if (authRequest != null) {
                        // Save the request in the cookie (crucial for OAuth2 state validation)
                        cookieRepo.saveAuthorizationRequest(authRequest, request, response);

                        // Output the auto-submitting POST form
                        String url = authRequest.getAuthorizationRequestUri();
                        response.setContentType("text/html;charset=UTF-8");
                        response.getWriter().write(buildAutoPostFormHtml(url));
                        return; // Stop the filter chain here, response is already committed
                    }
                }
                // For all other requests (or other providers), proceed normally
                filterChain.doFilter(request, response);
            }
        };

        http
                .cors(Customizer.withDefaults())
                .securityMatcher("/**")
                .sessionManagement(handler-> handler.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers( headers ->{
                    headers
                            .cacheControl(Customizer.withDefaults())
                            .frameOptions( fo-> fo.disable());
                })
                .csrf(csrf->csrf.disable())
                .formLogin(fl->fl.disable())
                .httpBasic(hb->hb.disable())
                .exceptionHandling(handler-> handler.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/",
                                "/error",
                                "/favicon.ico",
                                "/**.png", "/*/*.png",
                                "/**.gif","/*/*.gif",
                                "/**.svg","/*/*.svg",
                                "/**.jpg","/*/*.jpg",
                                "/**.html","/*/*.html",
                                "/**.css","/*/*.css",
                                "/**.js","/*/*.js",
                                "/auth/**", "/oauth2/**", "/logout",
                                "/api/public/**", "/api/entry/file/**",
                                "/api/cogna/*/file/**",
                                "/api/signa/*/file/**",
                                "/api/form/qr",
                                "/api/cogna/*/ingested-file/**",
                                "/api/app/path/**",
                                "/api/run/app/path/**",
                                "/api/app/logo/**", "/api/app/*/logo/**", "/api/app/*/manifest.json",
                                "/api/app/check-code-key",
                                "/api/app/check-by-key",
                                "/api/app/*/export",
                                "/api/push/**",
                                "/api/app/time",
                                "/api/cogna/export-log-csv",
                                "/api/cogna/*/export-log-csv",
                                "/api/krypta/*/verify-hash",
                                "/error",
                                "/api/at/clear-token",
                                "/api/lambda/*/out", "/~/**", "/$/**","/~cogna/**",
                                "/api/lambda/*/print",
                                "/user/*/photo/*",
                                "/api/bucket/zip-download/**",
                                "/report/**", "/token/get", "/px/**").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2Login->{
                    oauth2Login
                            .tokenEndpoint(token -> token.accessTokenResponseClient(bearerTokenResponseClient()))
                            .authorizationEndpoint(authEp->authEp
                                    .baseUri("/oauth2/authorize")
                                    .authorizationRequestResolver(resolver)
                                    .authorizationRequestRepository(cookieRepo)
                            )
                            .userInfoEndpoint(uie->uie.userService(customOAuth2UserService))
                            .successHandler(oAuth2AuthenticationSuccessHandler)
                            .failureHandler(oAuth2AuthenticationFailureHandler);
                })
                .logout(logout-> logout
                        .logoutUrl("/oauth2/logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                        .logoutSuccessHandler(new LogoutSuccessHandler(HttpStatus.OK))
                );

        // --- NEW: Insert our custom filter before the default Spring Security redirect filter ---
        http.addFilterBefore(sarawakIdPostFilter, OAuth2AuthorizationRequestRedirectFilter.class);

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        // 3. NEW: Intercepts the fully authenticated user and swaps the context if impersonation headers exist
        http.addFilterAfter(impersonationFilter, TokenAuthenticationFilter.class);

        return http.build();
    }


    public static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> bearerTokenResponseClient() {
        Converter<Map<String, Object>, OAuth2AccessTokenResponse> linkedinMapConverter = tokenResponse -> {
            var withTokenType = new HashMap<>(tokenResponse);
            withTokenType.put(OAuth2ParameterNames.TOKEN_TYPE, OAuth2AccessToken.TokenType.BEARER.getValue());
            return new DefaultMapOAuth2AccessTokenResponseConverter().convert(withTokenType);
        };

        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        RestClient restClient = RestClient.builder()
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new FormHttpMessageConverter());
                    var tokenConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
                    tokenConverter.setAccessTokenResponseConverter(linkedinMapConverter);
                    converters.add(tokenConverter);
                })
                .build();

        client.setRestClient(restClient);
        return client;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String buildAutoPostFormHtml(String fullUrl) {
        String[] parts = fullUrl.split("\\?");
        String actionUrl = parts[0];
        StringBuilder inputs = new StringBuilder();

        if (parts.length > 1) {
            String[] params = parts[1].split("&");
            for (String param : params) {
                String[] kv = param.split("=", 2);
                String key = kv[0];
                String value = kv.length > 1 ? kv[1] : "";

                String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8);
                inputs.append(String.format("         <input type=\"hidden\" name=\"%s\" value=\"%s\" />\n", key, decodedValue));
            }
        }

        return String.format("""
            <html>
               <body onload="document.forms[0].submit()">
                  <p>Redirecting to SarawakID...</p>
                  <form method="POST" action="%s">
            %s      </form>
               </body>
            </html>
            """, actionUrl, inputs.toString());
    }
}