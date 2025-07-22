package com.benzourry.leap.config;

//import com.benzourry.leap.CustomRequestEntityConverter;

import com.benzourry.leap.security.ApiKeyAuthFilter;
import com.benzourry.leap.security.CustomUserDetailsService;
import com.benzourry.leap.security.RestAuthenticationEntryPoint;
import com.benzourry.leap.security.TokenAuthenticationFilter;
import com.benzourry.leap.security.oauth2.CustomOAuth2UserService;
import com.benzourry.leap.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.benzourry.leap.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.benzourry.leap.security.oauth2.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.DispatcherType;
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
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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

    public SecurityFilterConfig(CustomUserDetailsService customUserDetailsService,
                                CustomOAuth2UserService customOAuth2UserService,
                                ClientRegistrationRepository clientRegistrationRepository,
                                OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                                ApiKeyAuthFilter authFilter,
                                HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.authFilter = authFilter;
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(customUserDetailsService);
    }

    /*
      By default, Spring OAuth2 uses HttpSessionOAuth2AuthorizationRequestRepository to save
      the authorization request. But, since our service is stateless, we can't save it in
      the session. We'll save the request in a Base64 encoded cookie instead.
    */
    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

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
                        "/auth/**", "/oauth2/**", "logout",
                        "/api/public/**", "/api/entry/file/**","/api/cogna/*/file/**", "/api/form/qr",
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
                            .authorizationRequestResolver(new CustomAuthorizationRequestResolver(
                                    this.clientRegistrationRepository))
                            .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                        )
                        .userInfoEndpoint(uie->uie.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler);

                })
                .logout(logout->{
                    logout.logoutRequestMatcher(new AntPathRequestMatcher("/oauth2/logout"))
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                        .logoutSuccessHandler(new LogoutSuccessHandler(HttpStatus.OK));
                });

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static DefaultAuthorizationCodeTokenResponseClient bearerTokenResponseClient() {
        var defaultMapConverter = new DefaultMapOAuth2AccessTokenResponseConverter();
        Converter<Map<String, Object>, OAuth2AccessTokenResponse> linkedinMapConverter = tokenResponse -> {
            var withTokenType = new HashMap<>(tokenResponse);
            withTokenType.put(OAuth2ParameterNames.TOKEN_TYPE, OAuth2AccessToken.TokenType.BEARER.getValue());
            return defaultMapConverter.convert(withTokenType);
        };

        var httpConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
        httpConverter.setAccessTokenResponseConverter(linkedinMapConverter);

        var restOperations = new RestTemplate(List.of(new FormHttpMessageConverter(), httpConverter));
        restOperations.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        var client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRestOperations(restOperations);
        return client;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.ignoring().antMatchers("/ignore1", "/ignore2");
//    }
//

//    @Bean
//    public DaoAuthenticationProvider getDaoAuthProvider(CustomUserDetailsService customDatabaseUserDetailsService) {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//        provider.setUserDetailsService(customDatabaseUserDetailsService);
//        provider.setPasswordEncoder(passwordEncoder());
//        return provider;
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Override
//    @Bean
//    public AuthenticationManager authenticationManagerBean() throws Exception {
//        return super.authenticationManagerBean();
//    }
//
//
//    @Bean(BeanIds.AUTHENTICATION_MANAGER)
//    @Override
//    public AuthenticationManager authenticationManagerBean() throws Exception {
//        return super.authenticationManagerBean();
//    }
//
//    @Configuration
//    @Order(1)
//    public static class EmbeddableWebPluginSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
//        protected void configure(HttpSecurity http) throws Exception {
//            // Disable X-Frame-Option Header
//            http.antMatcher("/api/entry/file/inline/**").headers().frameOptions().disable();
//        }
//    }

}
