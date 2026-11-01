package com.mfish.erp.whatsapp.webhook;

import com.mfish.erp.whatsapp.config.WhatsAppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Met le corps du webhook WhatsApp en cache (octets bruts) afin de verifier la signature
 * HMAC sur le corps exact avant toute deserialisation. Cree uniquement quand le module
 * WhatsApp est active (evite d'etre inclus dans les slices @WebMvcTest).
 */
@Component
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class CacheCorpsWebhookFilter extends OncePerRequestFilter {

    private final WhatsAppProperties properties;

    public CacheCorpsWebhookFilter(final WhatsAppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {
        if (estWebhookWhatsApp(request)) {
            filterChain.doFilter(
                    new ContentCachingRequestWrapper(request, properties.maxBodySizeBytes()),
                    response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean estWebhookWhatsApp(final HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/webhooks/whatsapp");
    }
}
