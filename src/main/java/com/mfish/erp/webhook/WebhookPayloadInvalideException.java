package com.mfish.erp.webhook;

/**
 * Levee lorsque le corps du webhook Chatwoot est malforme.
 */
public class WebhookPayloadInvalideException extends RuntimeException {

    public WebhookPayloadInvalideException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
