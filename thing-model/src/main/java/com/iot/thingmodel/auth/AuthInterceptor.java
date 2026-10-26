package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = Constants.Auth.CURRENT_USER_ATTRIBUTE;

    private final TbUserVerifier userVerifier;

    public AuthInterceptor(TbUserVerifier userVerifier) {
        this.userVerifier = userVerifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String token = request.getHeader(Constants.Auth.HEADER_X_AUTHORIZATION);
        if (token == null || token.isBlank()) {
            token = request.getHeader(Constants.Auth.HEADER_AUTHORIZATION);
        }
        if (token == null || token.isBlank()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED, "缺少访问令牌");
        }
        CurrentUser currentUser = userVerifier.verify(token);
        request.setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);
        checkAuthority(handler, currentUser);
        return true;
    }

    private void checkAuthority(Object handler, CurrentUser currentUser) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        RequireAuthority required = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireAuthority.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireAuthority.class);
        }
        if (required == null) {
            return;
        }
        boolean allowed = Arrays.stream(required.value())
                .anyMatch(authority -> authority.equalsIgnoreCase(currentUser.authority()));
        if (!allowed) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, Constants.ErrorCode.FORBIDDEN, "没有访问该接口的权限");
        }
    }
}
