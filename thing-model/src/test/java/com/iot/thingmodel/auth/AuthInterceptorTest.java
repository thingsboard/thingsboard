package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private final TbUserVerifier verifier = mock(TbUserVerifier.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(verifier);
    private final CurrentUser tenantAdmin = new CurrentUser("u1", UUID.randomUUID().toString(), "TENANT_ADMIN", "a@b");
    private static final String TOKEN_HEADER = "Bearer token";

    @Test
    void allowsMethodWithMatchingAuthority() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenReturn(tenantAdmin);
        MockHttpServletRequest request = request();
        HandlerMethod handler = handler(new TestController(), "tenantAllowed");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
        assertEquals(tenantAdmin, request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE));
    }

    @Test
    void rejectsMethodWithDifferentAuthority() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenReturn(tenantAdmin);
        HandlerMethod handler = handler(new TestController(), "adminOnly");

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request(), new MockHttpServletResponse(), handler));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void checksClassLevelAuthority() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenReturn(tenantAdmin);
        HandlerMethod handler = handler(new AdminOnlyController(), "doIt");

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request(), new MockHttpServletResponse(), handler));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void rejectsMissingToken() throws Exception {
        HandlerMethod handler = handler(new TestController(), "tenantAllowed");

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(
                        new MockHttpServletRequest(), new MockHttpServletResponse(), handler));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void acceptsAuthorizationHeaderFallback() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenReturn(tenantAdmin);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", TOKEN_HEADER);
        HandlerMethod handler = handler(new TestController(), "tenantAllowed");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
        assertEquals(tenantAdmin, request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE));
    }

    @Test
    void skipsOptionsRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products");

        assertTrue(interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void propagatesVerifierFailure() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "bad token"));
        HandlerMethod handler = handler(new TestController(), "tenantAllowed");

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(
                        request(), new MockHttpServletResponse(), handler));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void allowsNonHandlerMethod() throws Exception {
        when(verifier.verify(TOKEN_HEADER)).thenReturn(tenantAdmin);

        assertTrue(interceptor.preHandle(
                request(), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void matchesAuthorityIgnoringCase() throws Exception {
        CurrentUser lowerCase = new CurrentUser(
                "u1", UUID.randomUUID().toString(), "tenant_admin", "a@b");
        when(verifier.verify(TOKEN_HEADER)).thenReturn(lowerCase);
        HandlerMethod handler = handler(new TestController(), "tenantAllowed");

        assertTrue(interceptor.preHandle(
                request(), new MockHttpServletResponse(), handler));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Authorization", TOKEN_HEADER);
        return request;
    }

    private HandlerMethod handler(Object bean, String methodName) throws Exception {
        return new HandlerMethod(bean, bean.getClass().getMethod(methodName));
    }

    static class TestController {

        @RequireAuthority({"TENANT_ADMIN", "SYS_ADMIN"})
        public void tenantAllowed() {
        }

        @RequireAuthority("SYS_ADMIN")
        public void adminOnly() {
        }
    }

    @RequireAuthority("SYS_ADMIN")
    static class AdminOnlyController {

        public void doIt() {
        }
    }
}
