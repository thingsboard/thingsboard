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
