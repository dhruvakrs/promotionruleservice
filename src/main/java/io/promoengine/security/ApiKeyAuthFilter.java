package io.promoengine.security;

import io.promoengine.PromoEngineProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator/", "/actuator",
            "/swagger-ui", "/v3/api-docs", "/webjars"
    );

    private final TenantResolver tenantResolver;
    private final PromoEngineProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Allow public paths through without an API key
        String path = request.getServletPath();
        boolean isPublic = PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = properties.getSecurity().getHeaderName();
        if (headerName == null) headerName = "X-API-Key";
        String apiKey = request.getHeader(headerName);

        Optional<String> tenantId = tenantResolver.resolve(apiKey);
        if (tenantId.isPresent()) {
            request.setAttribute("tenantId", tenantId.get());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    tenantId.get(), apiKey, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } else {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"errorCode\":50,\"message\":\"Invalid or missing API key\"}");
        }
    }
}
