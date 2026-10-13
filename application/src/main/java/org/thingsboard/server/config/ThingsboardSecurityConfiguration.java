// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.AuthExceptionHandler;
import org.thingsboard.server.service.security.auth.extractor.TokenExtractor;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.JwtTokenAuthenticationProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.SkipPathRequestMatcher;
import org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.thingsboard.server.service.security.auth.pat.ApiKeyAuthenticationProvider;
import org.thingsboard.server.service.security.auth.pat.ApiKeyTokenAuthenticationProcessingFilter;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationProvider;
import org.thingsboard.server.service.security.auth.rest.RestLoginProcessingFilter;
import org.thingsboard.server.service.security.auth.rest.RestPublicLoginProcessingFilter;
import org.thingsboard.server.transport.http.config.PayloadSizeFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@TbCoreComponent
public class ThingsboardSecurityConfiguration {

    public static final String AUTHORIZATION_HEADER = "X-Authorization";
    public static final String AUTHORIZATION_HEADER_V2 = "Authorization";
    public static final String JWT_TOKEN_QUERY_PARAM = "token";

    public static final String API_KEY_HEADER_PREFIX = "ApiKey ";
    public static final String BEARER_HEADER_PREFIX = "Bearer ";

    /** The two prefixes that carry the management API: the platform's own REST API and the analytics proxy. */
    public static final String API_ENTRY_POINT = "/api/**";
    public static final String TRENDZ_API_ENTRY_POINT = "/apiTrendz/**";
    public static final String TRENDZ_CONNECT_ENTRY_POINT = "/api/trendz/public/connect";
    public static final String TRENDZ_PUBLIC_API_ENTRY_POINT = "/apiTrendz/publicApi/**";
    public static final String TRENDZ_WEB_APP_ENTRY_POINT = "/trendz/**";
    public static final String DEVICE_API_ENTRY_POINT = "/api/v1/**";
    public static final String FORM_BASED_LOGIN_ENTRY_POINT = "/api/auth/login";
    public static final String PUBLIC_LOGIN_ENTRY_POINT = "/api/auth/login/public";
    public static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/token";
    public static final String CURRENT_USER_ENTRY_POINT = "/api/auth/user";
    public static final String TWO_FACTOR_AUTH_ENTRY_POINT = "/api/auth/2fa/**";
    public static final String LICENSE_KEY_ENTRY_POINT = "/api/admin/license/key";
    /** Open while locked for the same reason as the key itself: reading a key before committing to it. */
    public static final String LICENSE_PREVIEW_ENTRY_POINT = "/api/admin/license/preview";
    public static final String NON_PRODUCTION_CONFIRM_ENTRY_POINT = "/api/admin/nonProduction/confirm";
    /** Exempt while locked because a locked instance is exactly when an administrator needs to reset it. */
    public static final String CLEAR_LICENSE_ENTRY_POINT = "/api/admin/license/clear";
    public static final String LOGOUT_ENTRY_POINT = "/api/auth/logout";
    public static final String SETUP_ENTRY_POINT = "/api/noauth/setup/**";
    public static final String DEVELOPMENT_MODE_PROBE_ENTRY_POINT = "/api/noauth/system/development";
    /**
     * What the login page loads before anyone signs in. Listed path by path rather than as the whole
     * {@code /api/noauth/**} prefix, which would also exempt the optional flows this filter keeps locked.
     */
    public static final String LOGIN_WHITE_LABELING_ENTRY_POINT = "/api/noauth/whiteLabel/loginWhiteLabelParams";
    /**
     * The image bytes that branding points at. Two single-segment wildcards and not {@code /**}: the mappings
     * take exactly an image type and a key.
     */
    public static final String LOGIN_LOGO_ENTRY_POINT = "/api/noauth/whiteLabel/loginLogo/*/*";
    public static final String LOGIN_FAVICON_ENTRY_POINT = "/api/noauth/whiteLabel/loginFavicon/*/*";
    public static final String SELF_REGISTRATION_PARAMS_ENTRY_POINT = "/api/noauth/selfRegistration/signUpSelfRegistrationParams";
    public static final String OAUTH2_CLIENTS_ENTRY_POINT = "/api/noauth/oauth2Clients";
    /**
     * Mandatory two-factor enrolment, which a sign-in can be forced through before any session is issued.
     * These live under {@code /api/2fa}, so {@link #TWO_FACTOR_AUTH_ENTRY_POINT} does not cover them; the
     * exempt set is exactly what the enrolment token is authorized for.
     */
    public static final String TWO_FACTOR_AUTH_PROVIDERS_ENTRY_POINT = "/api/2fa/providers";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_SETTINGS_ENTRY_POINT = "/api/2fa/account/settings";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_CONFIG_GENERATE_ENTRY_POINT = "/api/2fa/account/config/generate";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_CONFIG_SUBMIT_ENTRY_POINT = "/api/2fa/account/config/submit";
    public static final String TWO_FACTOR_AUTH_ACCOUNT_CONFIG_ENTRY_POINT = "/api/2fa/account/config";
    /**
     * The expired-password reset a sign-in can be forced through. {@link #RESET_PASSWORD_ENTRY_POINT} is
     * exempt for POST only - GET on the same template is the optional forgot-my-password landing page.
     */
    public static final String USER_PASSWORD_POLICY_ENTRY_POINT = "/api/noauth/userPasswordPolicy";
    public static final String RESET_PASSWORD_ENTRY_POINT = "/api/noauth/resetPassword";
    /**
     * GET /api/user/{userId} - the read the sign-in flow completes with. GET only, since {@code DELETE} shares
     * the path template, and a single identifier-shaped segment rather than {@code /api/user/*}, which would
     * also match the paged user list and the other listings under this prefix.
     */
    public static final String CURRENT_USER_BY_ID_ENTRY_POINT = "/api/user/{userId:[0-9a-fA-F-]{36}}";
    public static final String SYSTEM_PARAMS_ENTRY_POINT = "/api/system/params";
    /** Session bootstrap, exempt for GET only: {@code POST} on the same path saves white-labeling configuration. */
    public static final String WHITE_LABELING_PARAMS_ENTRY_POINT = "/api/whiteLabel/whiteLabelParams";
    /** Session bootstrap, exempt for GET only: {@code POST} on the same path creates a custom menu. */
    public static final String CUSTOM_MENU_ENTRY_POINT = "/api/customMenu";
    public static final String ALLOWED_PERMISSIONS_ENTRY_POINT = "/api/permissions/allowedPermissions";
    protected static final String[] NON_TOKEN_BASED_AUTH_ENTRY_POINTS = new String[]{"/index.html", "/assets/**", "/static/**", "/api/noauth/**", "/webjars/**", "/api/license/**", "/api/images/public/**", "/api/v2/report/public/**", "/.well-known/**"};
    /**
     * API entry points that stay reachable while the setup is incomplete, or while a provisioned instance is
     * locked again at runtime. Only {@link #SETUP_PROTECTED_ENTRY_POINTS} is locked, so the Angular
     * application and its assets are always served and the activation UI stays reachable.
     * <p>
     * The test for adding an entry: does the platform <em>force</em> a system administrator through this
     * request in order to reach a completed sign-in? A forced interstitial - mandatory two-factor enrolment,
     * an expired password - belongs here; anything a user merely <em>may</em> choose to do - self-registration,
     * forgot-my-password, account activation, managing an already-configured two-factor provider, custom
     * translations - stays locked. Where a forced request shares a path template with an optional one, restrict
     * by {@link HttpMethod} rather than widening the path.
     */
    public static final List<RequestMatcher> SETUP_ALLOWED_ENTRY_POINTS = List.of(
            entryPoint(SETUP_ENTRY_POINT), entryPoint(DEVELOPMENT_MODE_PROBE_ENTRY_POINT),
            entryPoint(DEVICE_API_ENTRY_POINT),
            entryPoint(LOGIN_WHITE_LABELING_ENTRY_POINT), entryPoint(LOGIN_LOGO_ENTRY_POINT), entryPoint(LOGIN_FAVICON_ENTRY_POINT),
            entryPoint(SELF_REGISTRATION_PARAMS_ENTRY_POINT), entryPoint(OAUTH2_CLIENTS_ENTRY_POINT),
            entryPoint(FORM_BASED_LOGIN_ENTRY_POINT), entryPoint(TOKEN_REFRESH_ENTRY_POINT), entryPoint(CURRENT_USER_ENTRY_POINT),
            entryPoint(TWO_FACTOR_AUTH_ENTRY_POINT), entryPoint(LOGOUT_ENTRY_POINT),
            entryPoint(TWO_FACTOR_AUTH_PROVIDERS_ENTRY_POINT), entryPoint(TWO_FACTOR_AUTH_ACCOUNT_SETTINGS_ENTRY_POINT),
            entryPoint(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_GENERATE_ENTRY_POINT), entryPoint(TWO_FACTOR_AUTH_ACCOUNT_CONFIG_SUBMIT_ENTRY_POINT),
            entryPoint(USER_PASSWORD_POLICY_ENTRY_POINT),
            entryPoint(SYSTEM_PARAMS_ENTRY_POINT), entryPoint(ALLOWED_PERMISSIONS_ENTRY_POINT),
            entryPoint(LICENSE_KEY_ENTRY_POINT), entryPoint(LICENSE_PREVIEW_ENTRY_POINT),
            entryPoint(NON_PRODUCTION_CONFIRM_ENTRY_POINT),
            entryPoint(CLEAR_LICENSE_ENTRY_POINT),
            // Public by design, and left open unauthenticated at the Spring Security layer as well.
            entryPoint(TRENDZ_PUBLIC_API_ENTRY_POINT), entryPoint(TRENDZ_WEB_APP_ENTRY_POINT),
            entryPoint(HttpMethod.GET, CURRENT_USER_BY_ID_ENTRY_POINT),
            entryPoint(HttpMethod.GET, WHITE_LABELING_PARAMS_ENTRY_POINT),
            entryPoint(HttpMethod.GET, CUSTOM_MENU_ENTRY_POINT),
            // POST only: PUT and DELETE on this template manage an already-configured provider and stay locked.
            entryPoint(HttpMethod.POST, TWO_FACTOR_AUTH_ACCOUNT_CONFIG_ENTRY_POINT),
            entryPoint(HttpMethod.POST, RESET_PASSWORD_ENTRY_POINT));
    /**
     * The surface {@link SystemSetupFilter} locks while the setup is incomplete: the management API and the
     * analytics proxy in front of it, minus {@link #SETUP_ALLOWED_ENTRY_POINTS}.
     */
    public static final List<RequestMatcher> SETUP_PROTECTED_ENTRY_POINTS = List.of(
            entryPoint(API_ENTRY_POINT), entryPoint(TRENDZ_API_ENTRY_POINT));
    public static final String[] TOKEN_BASED_AUTH_ENTRY_POINTS = new String[]{API_ENTRY_POINT, TRENDZ_API_ENTRY_POINT};
    protected static final String[] TRENDZ_NON_TOKEN_BASED_AUTH_ENTRY_POINTS = new String[]{TRENDZ_CONNECT_ENTRY_POINT, TRENDZ_PUBLIC_API_ENTRY_POINT, TRENDZ_WEB_APP_ENTRY_POINT};
    public static final String WS_ENTRY_POINT = "/api/ws/**";
    public static final String MAIL_OAUTH2_PROCESSING_ENTRY_POINT = "/api/admin/mail/oauth2/code";
    public static final String DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT = "/api/device-connectivity/*/certificate/download";

    private static RequestMatcher entryPoint(String pattern) {
        return PathPatternRequestMatcher.withDefaults().matcher(pattern);
    }

    private static RequestMatcher entryPoint(HttpMethod method, String pattern) {
        return PathPatternRequestMatcher.withDefaults().matcher(method, pattern);
    }

    @Value("${server.http.max_payload_size:/api/image*/**=52428800;/api/resource/**=52428800;/api/**=16777216}")
    private String maxPayloadSizeConfig;

    @Autowired
    private ThingsboardErrorResponseHandler restAccessDeniedHandler;

    @Autowired(required = false)
    @Qualifier("oauth2AuthenticationSuccessHandler")
    private AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired(required = false)
    @Qualifier("oauth2AuthenticationFailureHandler")
    private AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Autowired(required = false)
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Autowired
    @Qualifier("defaultAuthenticationSuccessHandler")
    private AuthenticationSuccessHandler successHandler;

    @Autowired
    @Qualifier("defaultAuthenticationFailureHandler")
    private AuthenticationFailureHandler failureHandler;

    @Autowired
    private RestAuthenticationProvider restAuthenticationProvider;
    @Autowired
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @Autowired
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;
    @Autowired
    private ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

    @Autowired(required = false)
    OAuth2Configuration oauth2Configuration;

    @Autowired
    @Qualifier("jwtHeaderTokenExtractor")
    private TokenExtractor jwtHeaderTokenExtractor;

    @Autowired
    @Qualifier("apiKeyHeaderTokenExtractor")
    private TokenExtractor apiKeyHeaderTokenExtractor;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RateLimitProcessingFilter rateLimitProcessingFilter;

    @Autowired
    private SystemSetupFilter systemSetupFilter;

    @Autowired
    private NonProductionHeaderFilter nonProductionHeaderFilter;

    @Autowired
    private AuthExceptionHandler authExceptionHandler;

    @Autowired
    private HttpSecurityHeadersCustomizer httpSecurityHeadersCustomizer;

    @Bean
    protected PayloadSizeFilter payloadSizeFilter() {
        return new PayloadSizeFilter(maxPayloadSizeConfig);
    }

    @Bean
    protected FilterRegistrationBean<ShallowEtagHeaderFilter> buildEtagFilter() {
        ShallowEtagHeaderFilter etagFilter = new ShallowEtagHeaderFilter();
        etagFilter.setWriteWeakETag(true);
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean
                = new FilterRegistrationBean<>(etagFilter);
        filterRegistrationBean.addUrlPatterns("*.js", "*.css", "*.ico", "/assets/*", "/static/*");
        filterRegistrationBean.setName("etagFilter");
        return filterRegistrationBean;
    }

    @Bean
    protected RestLoginProcessingFilter buildRestLoginProcessingFilter() {
        RestLoginProcessingFilter filter = new RestLoginProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected RestPublicLoginProcessingFilter buildRestPublicLoginProcessingFilter() {
        RestPublicLoginProcessingFilter filter = new RestPublicLoginProcessingFilter(PUBLIC_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter() {
        SkipPathRequestMatcher matcher = buildSkipPathRequestMatcher();
        JwtTokenAuthenticationProcessingFilter filter
                = new JwtTokenAuthenticationProcessingFilter(failureHandler, jwtHeaderTokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected ApiKeyTokenAuthenticationProcessingFilter buildApiKeyTokenAuthenticationProcessingFilter() {
        SkipPathRequestMatcher matcher = buildSkipPathRequestMatcher();
        ApiKeyTokenAuthenticationProcessingFilter filter =
                new ApiKeyTokenAuthenticationProcessingFilter(failureHandler, apiKeyHeaderTokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    private SkipPathRequestMatcher buildSkipPathRequestMatcher() {
        List<String> pathsToSkip = Stream.concat(
                Stream.concat(
                        Arrays.stream(NON_TOKEN_BASED_AUTH_ENTRY_POINTS),
                        Arrays.stream(TRENDZ_NON_TOKEN_BASED_AUTH_ENTRY_POINTS)
                ),
                Stream.of(
                        WS_ENTRY_POINT,
                        TOKEN_REFRESH_ENTRY_POINT,
                        FORM_BASED_LOGIN_ENTRY_POINT,
                        PUBLIC_LOGIN_ENTRY_POINT,
                        DEVICE_API_ENTRY_POINT,
                        MAIL_OAUTH2_PROCESSING_ENTRY_POINT,
                        DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT)
        ).toList();
        List<String> pathToProcess = Arrays.stream(TOKEN_BASED_AUTH_ENTRY_POINTS).toList();
        return new SkipPathRequestMatcher(pathsToSkip, pathToProcess);
    }

    @Bean
    protected RefreshTokenProcessingFilter buildRefreshTokenProcessingFilter() {
        RefreshTokenProcessingFilter filter = new RefreshTokenProcessingFilter(TOKEN_REFRESH_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(
                restAuthenticationProvider,
                jwtAuthenticationProvider,
                apiKeyAuthenticationProvider,
                refreshTokenAuthenticationProvider
        ));
    }

    @Autowired
    private OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver;

    @Bean
    @Order(0)
    SecurityFilterChain resources(HttpSecurity http) throws Exception {
        http
                .securityMatchers(matchers -> matchers
                        .requestMatchers("/*.js", "/*.css", "/*.ico", "/assets/**", "/static/**"))
                .headers(headers -> {
                    headers.defaultsDisabled();
                    headers.addHeaderWriter(new StaticHeadersWriter(HttpHeaders.CACHE_CONTROL, "max-age=0, public"));
                    httpSecurityHeadersCustomizer.customize(headers);
                })
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll())
                .requestCache(RequestCacheConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> {
                    headers.defaultsDisabled();
                    headers.cacheControl(config -> {});
                    headers.crossOriginOpenerPolicy(coop -> coop.policy(CrossOriginOpenerPolicy.SAME_ORIGIN));
                    httpSecurityHeadersCustomizer.customize(headers);
                })
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(config -> {})
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(config -> config
                        .requestMatchers(NON_TOKEN_BASED_AUTH_ENTRY_POINTS).permitAll() // static resources, user activation and password reset end-points (webjars included)
                        .requestMatchers(TRENDZ_NON_TOKEN_BASED_AUTH_ENTRY_POINTS).permitAll() // Trendz resources and end-points
                        .requestMatchers(
                                FORM_BASED_LOGIN_ENTRY_POINT, // Login end-point
                                PUBLIC_LOGIN_ENTRY_POINT, // Public login end-point
                                TOKEN_REFRESH_ENTRY_POINT, // Token refresh end-point
                                MAIL_OAUTH2_PROCESSING_ENTRY_POINT, // Mail oauth2 code processing url
                                DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT, // Device connectivity certificate (public)
                                WS_ENTRY_POINT).permitAll() // Protected WebSocket API End-points
                        .requestMatchers(TOKEN_BASED_AUTH_ENTRY_POINTS).authenticated() // Protected API End-points
                        .anyRequest().permitAll())
                .exceptionHandling(config -> config.accessDeniedHandler(restAccessDeniedHandler))
                // Ahead of systemSetupFilter - and so of every authentication filter - so the header lands
                // even when the setup lock short-circuits the chain. Anchored one static step earlier, on
                // CsrfFilter: sharing LogoutFilter's anchor would leave their relative order to an equal-order
                // tie, and SystemSetupFilter.class has no registered order until the call below runs.
                .addFilterBefore(nonProductionHeaderFilter, CsrfFilter.class)
                .addFilterBefore(systemSetupFilter, LogoutFilter.class)
                .addFilterBefore(buildRestLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRestPublicLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildApiKeyTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRefreshTokenProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(payloadSizeFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authExceptionHandler, buildRestLoginProcessingFilter().getClass());
        if (oauth2Configuration != null) {
            http.oauth2Login(login -> login
                    .authorizationEndpoint(config -> config
                            .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                            .authorizationRequestResolver(oAuth2AuthorizationRequestResolver))
                    .loginPage("/oauth2Login")
                    .loginProcessingUrl(oauth2Configuration.getLoginProcessingUrl())
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(oauth2AuthenticationFailureHandler));
        }
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(CorsFilter.class)
    public CorsFilter corsFilter(@Autowired MvcCorsProperties mvcCorsProperties) {
        if (mvcCorsProperties.getMappings().isEmpty()) {
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        } else {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setCorsConfigurations(mvcCorsProperties.getMappings());
            return new CorsFilter(source);
        }
    }

}
