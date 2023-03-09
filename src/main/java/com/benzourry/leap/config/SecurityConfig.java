//package com.benzourry.leap.config;
//
////import com.benzourry.leap.CustomRequestEntityConverter;
//import com.benzourry.leap.security.CustomUserDetailsService;
//import com.benzourry.leap.security.RestAuthenticationEntryPoint;
//import com.benzourry.leap.security.TokenAuthenticationFilter;
//import com.benzourry.leap.security.oauth2.CustomOAuth2UserService;
//import com.benzourry.leap.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
//import com.benzourry.leap.security.oauth2.OAuth2AuthenticationFailureHandler;
//import com.benzourry.leap.security.oauth2.OAuth2AuthenticationSuccessHandler;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.converter.FormHttpMessageConverter;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.BeanIds;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
//import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
//import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
//import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Arrays;
//
//@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(
//        securedEnabled = true,
//        jsr250Enabled = true,
//        prePostEnabled = true
//)
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//    private final CustomUserDetailsService customUserDetailsService;
//
//    private final CustomOAuth2UserService customOAuth2UserService;
//
//    private final ClientRegistrationRepository clientRegistrationRepository;
//
//    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
//
//    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
//
//    public SecurityConfig(CustomUserDetailsService customUserDetailsService, CustomOAuth2UserService customOAuth2UserService, ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler, HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
//        this.customUserDetailsService = customUserDetailsService;
//        this.customOAuth2UserService = customOAuth2UserService;
//        this.clientRegistrationRepository = clientRegistrationRepository;
//        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
//        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
//    }
//
//    @Bean
//    public TokenAuthenticationFilter tokenAuthenticationFilter() {
//        return new TokenAuthenticationFilter();
//    }
//
//    /*
//      By default, Spring OAuth2 uses HttpSessionOAuth2AuthorizationRequestRepository to save
//      the authorization request. But, since our service is stateless, we can't save it in
//      the session. We'll save the request in a Base64 encoded cookie instead.
//    */
//    @Bean
//    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
//        return new HttpCookieOAuth2AuthorizationRequestRepository();
//    }
//
//    @Override
//    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
//        authenticationManagerBuilder
//                .userDetailsService(customUserDetailsService)
//                .passwordEncoder(passwordEncoder());
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
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
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .cors()
//                    .and()
//                .sessionManagement()
//                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                    .and()
//                .headers()
//                    .cacheControl()
//                .and()
//                    .frameOptions()
//                    .sameOrigin()
////                .and()
////                .antMatcher("/api/entry/file/inline/**")
////                    .headers()
////                    .frameOptions()
////                    .disable()
////                    .frameOptions().disable()
////                    .addHeaderWriter(new XFrameOptionsHeaderWriter(new WhiteListedAllowFromStrategy(Arrays.asList(
////                            "ia.unimas.my",
////                            "*.ia.unimas.my",
////                            "reka.unimas.my",
////                            "*.reka.unimas.my",
////                            "leap.my",
////                            "*.leap.my"))))
//                    .and()
//                .csrf()
//                    .disable()
//                .formLogin()
//                    .disable()
//                .httpBasic()
//                    .disable()
//                .exceptionHandling()
//                    .authenticationEntryPoint(new RestAuthenticationEntryPoint())
//                    .and()
//                .authorizeRequests()
//                    .antMatchers("/",
//                        "/error",
//                        "/favicon.ico",
//                        "/**/*.png",
//                        "/**/*.gif",
//                        "/**/*.svg",
//                        "/**/*.jpg",
//                        "/**/*.html",
//                        "/**/*.css",
//                        "/**/*.js")
//                        .permitAll()
//                    .antMatchers("/auth/**", "/oauth2/**","logout")
//                        .permitAll()
//                .antMatchers("/api/public/**","/api/entry/file/**","/api/form/qr",
////                        "/api/entry/**",
////                        "/api/endpoint/**",
//                        "/api/app/path/**","/api/app/logo/**","/api/app/**/logo/**","/api/app/**/manifest.json",
//                        "/api/app/check-code-key",
//                        "/api/push/**",
//                        "/api/at/clear-token",
//                        "/api/lambda/**/out","/~/**","/$/**",
//                        "/api/lambda/**/print",
//                        "/report/**","/token/get").permitAll()
//                    .anyRequest()
//                        .authenticated()
//                    .and()
//                .oauth2Login()
//                    .authorizationEndpoint()
//                        .baseUri("/oauth2/authorize")
//                .authorizationRequestResolver(
//                            new CustomAuthorizationRequestResolver(
//                                this.clientRegistrationRepository))
//                        .authorizationRequestRepository(cookieAuthorizationRequestRepository())
//                        .and()
////                    .redirectionEndpoint()
////                        .baseUri("/login/oauth2/code/*")
////                        .baseUri("/oauth2/callback/*")
////                        .and()
//                    .userInfoEndpoint()
//                        .userService(customOAuth2UserService)
//                        .and()
////                    .tokenEndpoint()
////                        .accessTokenResponseClient(authorizationCodeTokenResponseClient())
////                    .and()
//                    .successHandler(oAuth2AuthenticationSuccessHandler)
//                    .failureHandler(oAuth2AuthenticationFailureHandler)
//                .and()
//                .logout()
//                    .logoutRequestMatcher(new AntPathRequestMatcher("/oauth2/logout"))
//                    .clearAuthentication(true)
//                    .invalidateHttpSession(true)
//                    .deleteCookies("JSESSIONID")
//                    .permitAll()
//                    .logoutSuccessHandler(new LogoutSuccessHandler(HttpStatus.OK));
//                  //  .logoutSuccessUrl("/");
//
//
//        // Add our custom Token based authentication filter
//        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
//    }
//
////    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
////        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
////                new OAuth2AccessTokenResponseHttpMessageConverter();
////        tokenResponseHttpMessageConverter.setTokenResponseConverter(new OAuth2AccessTokenResponseConverterWithDefaults());
////
////        RestTemplate restTemplate = new RestTemplate(Arrays.asList(
////                new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));
////        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
////
////        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
////        tokenResponseClient.setRestOperations(restTemplate);
////        tokenResponseClient.setRequestEntityConverter(new CustomRequestEntityConverter());
////
////        return tokenResponseClient;
////    }
//}
