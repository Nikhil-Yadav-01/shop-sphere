package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestValidationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var method = request.getMethod();
        var contentType = request.getHeaders().getContentType();

        // Validate Content-Type for request body methods
        if ((HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method))) {
            if (request.getHeaders().getContentLength() > 0 && contentType == null) {
                logger.warn("POST/PUT/PATCH request without Content-Type header");
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }

            // Validate supported Content-Type
            if (contentType != null && !isSupportedContentType(contentType)) {
                logger.warn("Unsupported Content-Type: {}", contentType);
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return exchange.getResponse().setComplete();
            }
        }

        // Validate Authorization header for protected paths
        String path = request.getPath().toString();
        if (isProtectedPath(path) && request.getHeaders().getFirst("Authorization") == null) {
            logger.warn("Protected path {} accessed without Authorization header", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isSupportedContentType(MediaType contentType) {
        return contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
               contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED) ||
               contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA) ||
               contentType.isCompatibleWith(MediaType.APPLICATION_XML) ||
               contentType.isCompatibleWith(new MediaType("application", "ld+json"));
    }

    private boolean isProtectedPath(String path) {
        return path.startsWith("/order/") ||
               path.startsWith("/checkout/") ||
               path.startsWith("/user/") ||
               path.startsWith("/admin/") ||
               path.startsWith("/notification/") ||
               path.startsWith("/returns/");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 8;
    }
}
