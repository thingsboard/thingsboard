package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

public interface NotificationListener {

    /**
     * Invoked when a notification for an observed resource has been received.
     *
     * @param request
     *            The original request that was used to establish the
     *            observation.
     * @param response
     *            the notification.
     */
    void onNotification(Request request, Response response);
}