/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.thingsboard.server.dao.audit.AuditLogLevelFilter;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.JwtTokenAuthenticationProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.SkipPathRequestMatcher;
import org.thingsboard.server.service.security.auth.jwt.extractor.TokenExtractor;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationProvider;
import org.thingsboard.server.service.security.auth.rest.RestLoginProcessingFilter;
import org.thingsboard.server.service.security.auth.rest.RestPublicLoginProcessingFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
@Order(SecurityProperties.BASIC_AUTH_ORDER)
public class ThingsboardSecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    public static final String JWT_TOKEN_QUERY_PARAM = "token";

    public static final String WEBJARS_ENTRY_POINT = "/webjars/**";
    public static final String DEVICE_API_ENTRY_POINT = "/api/v1/**";
    public static final String FORM_BASED_LOGIN_ENTRY_POINT = "/api/auth/login";
    public static final String PUBLIC_LOGIN_ENTRY_POINT = "/api/auth/login/public";
    public static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/token";
    protected static final String[] NON_TOKEN_BASED_AUTH_ENTRY_POINTS = new String[] {"/index.html", "/static/**", "/api/noauth/**", "/webjars/**"};
    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
    public static final String WS_TOKEN_BASED_AUTH_ENTRY_POINT = "/api/ws/**";

    @Autowired private ThingsboardErrorResponseHandler restAccessDeniedHandler;

    @Autowired(required = false)
    @Qualifier("oauth2AuthenticationSuccessHandler")
    private AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    @Qualifier("defaultAuthenticationSuccessHandler")
    private AuthenticationSuccessHandler successHandler;

    @Autowired private AuthenticationFailureHandler failureHandler;
    @Autowired private RestAuthenticationProvider restAuthenticationProvider;
    @Autowired private JwtAuthenticationProvider jwtAuthenticationProvider;
    @Autowired private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    @Autowired(required = false) OAuth2Configuration oauth2Configuration;

    @Autowired
    @Qualifier("jwtHeaderTokenExtractor")
    private TokenExtractor jwtHeaderTokenExtractor;

    @Autowired
    @Qualifier("jwtQueryTokenExtractor")
    private TokenExtractor jwtQueryTokenExtractor;

    @Autowired private AuthenticationManager authenticationManager;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private RateLimitProcessingFilter rateLimitProcessingFilter;

    @Bean
    protected RestLoginProcessingFilter buildRestLoginProcessingFilter() throws Exception {
        RestLoginProcessingFilter filter = new RestLoginProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler, failureHandler, objectMapper);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected RestPublicLoginProcessingFilter buildRestPublicLoginProcessingFilter() throws Exception {
        RestPublicLoginProcessingFilter filter = new RestPublicLoginProcessingFilter(PUBLIC_LOGIN_ENTRY_POINT, successHandler, failureHandler, objectMapper);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter() throws Exception {
        List<String> pathsToSkip = new ArrayList(Arrays.asList(NON_TOKEN_BASED_AUTH_ENTRY_POINTS));
        pathsToSkip.addAll(Arrays.asList(WS_TOKEN_BASED_AUTH_ENTRY_POINT, TOKEN_REFRESH_ENTRY_POINT, FORM_BASED_LOGIN_ENTRY_POINT,
                PUBLIC_LOGIN_ENTRY_POINT, DEVICE_API_ENTRY_POINT, WEBJARS_ENTRY_POINT));
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter
                = new JwtTokenAuthenticationProcessingFilter(failureHandler, jwtHeaderTokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected RefreshTokenProcessingFilter buildRefreshTokenProcessingFilter() throws Exception {
        RefreshTokenProcessingFilter filter = new RefreshTokenProcessingFilter(TOKEN_REFRESH_ENTRY_POINT, successHandler, failureHandler, objectMapper);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    protected JwtTokenAuthenticationProcessingFilter buildWsJwtTokenAuthenticationProcessingFilter() throws Exception {
        AntPathRequestMatcher matcher = new AntPathRequestMatcher(WS_TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter
                = new JwtTokenAuthenticationProcessingFilter(failureHandler, jwtQueryTokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(restAuthenticationProvider);
        auth.authenticationProvider(jwtAuthenticationProvider);
        auth.authenticationProvider(refreshTokenAuthenticationProvider);
    }

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/static/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().cacheControl().and().frameOptions().disable()
                .and()
                .cors()
                .and()
                .csrf().disable()
                .exceptionHandling()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(WEBJARS_ENTRY_POINT).permitAll() // Webjars
                .antMatchers(DEVICE_API_ENTRY_POINT).permitAll() // Device HTTP Transport API
                .antMatchers(FORM_BASED_LOGIN_ENTRY_POINT).permitAll() // Login end-point
                .antMatchers(PUBLIC_LOGIN_ENTRY_POINT).permitAll() // Public login end-point
                .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll() // Token refresh end-point
                .antMatchers(NON_TOKEN_BASED_AUTH_ENTRY_POINTS).permitAll() // static resources, user activation and password reset end-points
                .and()
                .authorizeRequests()
                .antMatchers(WS_TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected WebSocket API End-points
                .antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected API End-points
                .and()
                .exceptionHandling().accessDeniedHandler(restAccessDeniedHandler)
                .and()
                .addFilterBefore(buildRestLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRestPublicLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRefreshTokenProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildWsJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitProcessingFilter, UsernamePasswordAuthenticationFilter.class);
        if (oauth2Configuration.isEnabled()) {
            http.oauth2Login()
                    .loginPage("/oauth2Login")
                    .loginProcessingUrl(oauth2Configuration.getClients().values().iterator().next().getLoginProcessingUrl())
                    .successHandler(oauth2AuthenticationSuccessHandler);
//                    .and().oauth2Login().loginProcessingUrl();
        }
    }


    @Bean
    @ConditionalOnMissingBean(CorsFilter.class)
    public CorsFilter corsFilter(@Autowired MvcCorsProperties mvcCorsProperties) {
        if (mvcCorsProperties.getMappings().size() == 0) {
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        } else {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setCorsConfigurations(mvcCorsProperties.getMappings());
            return new CorsFilter(source);
        }
    }

    @Bean
    public AuditLogLevelFilter auditLogLevelFilter(@Autowired AuditLogLevelProperties auditLogLevelProperties) {
        return new AuditLogLevelFilter(auditLogLevelProperties.getMask());
    }
}
