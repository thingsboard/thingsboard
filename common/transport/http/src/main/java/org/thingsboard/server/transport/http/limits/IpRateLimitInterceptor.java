package org.thingsboard.server.transport.http.limits;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.thingsboard.server.transport.http.HttpTransportContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetSocketAddress;
import java.util.Enumeration;

public class IpRateLimitInterceptor implements HandlerInterceptor {
    private final HttpTransportContext ctx;

    public IpRateLimitInterceptor(HttpTransportContext context){
        this.ctx = context;
    }
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (ctx.checkAddress(getInetSocketAddress(request))){
            return true;
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {
        int status = response.getStatus();
        InetSocketAddress addr = getInetSocketAddress(request);
        if (status != HttpStatus.UNAUTHORIZED.value()){
            ctx.onAuthSuccess(addr);
        } else {
            ctx.onAuthFailure(addr);
        }
    }

    @NotNull
    private InetSocketAddress getInetSocketAddress(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders("X-Forwarded-For");
        String addr;
        if (headers.hasMoreElements()) {
            addr = headers.nextElement();
        } else {
            addr = request.getRemoteAddr();
        }
        return new InetSocketAddress(addr, request.getRemotePort());
    }

}