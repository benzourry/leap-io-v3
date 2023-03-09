package com.benzourry.leap.config;

//import com.benzourry.leap.CustomRequestEntityConverter;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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

    public SecurityFilterConfig(CustomUserDetailsService customUserDetailsService,
                                CustomOAuth2UserService customOAuth2UserService,
                                ClientRegistrationRepository clientRegistrationRepository,
                                OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                                HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
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
                .cors()
                .and()
            .securityMatcher("/**")
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .headers()
                .cacheControl()
                .and()
                .frameOptions()
                    .sameOrigin()
                .and()
                .csrf()
                    .disable()
            .formLogin()
                .disable()
            .httpBasic()
                .disable()
            .exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
            .and()
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
                        "/api/public/**", "/api/entry/file/**", "/api/form/qr",
                        "/api/app/path/**", "/api/app/logo/**", "/api/app/*/logo/**", "/api/app/*/manifest.json",
                        "/api/app/check-code-key",
                        "/api/app/check-by-key",
                        "/api/push/**",
                        "/error",
                        "/api/at/clear-token",
                        "/api/lambda/*/out", "/~/**", "/$/**",
                        "/api/lambda/*/print",
                        "/report/*", "/token/get", "/px/**").permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login()
                .authorizationEndpoint()
                .baseUri("/oauth2/authorize")
            .authorizationRequestResolver(
                    new CustomAuthorizationRequestResolver(
                            this.clientRegistrationRepository))
            .authorizationRequestRepository(cookieAuthorizationRequestRepository())
            .and()
            .userInfoEndpoint()
            .userService(customOAuth2UserService)
            .and()
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
            .and()
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/oauth2/logout"))
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            .logoutSuccessHandler(new LogoutSuccessHandler(HttpStatus.OK));

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
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
