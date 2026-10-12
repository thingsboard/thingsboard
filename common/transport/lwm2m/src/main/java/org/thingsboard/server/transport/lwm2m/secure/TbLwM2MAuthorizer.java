// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.Authorization;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import java.net.InetSocketAddress;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@TbLwM2mTransportComponent
@Slf4j
public class TbLwM2MAuthorizer implements Authorizer {

    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final TbMainSecurityStore securityStore;
    private final SecurityChecker securityChecker = new SecurityChecker();
    private final LwM2mClientContext clientContext;
    private final TransportRateLimitService rateLimitService;

    @Override
    public Authorization isAuthorized(UplinkRequest<?> request, Registration registration, LwM2mPeer sender) {
        InetSocketAddress address = sender instanceof IpPeer ipPeer ? ipPeer.getSocketAddress() : null;
        // Like a block of MQTT connections: registered clients keep updating their registrations
        if (request instanceof RegisterRequest && address != null && !rateLimitService.checkAddress(address)) {
            log.debug("[{}][{}] Registration declined: IP address is blocked due to repeatedly failed authentication", registration.getEndpoint(), address);
            return Authorization.declined();
        }
        Authorization authorization;
        try {
            authorization = authorize(registration, sender);
        } catch (LwM2MTransportUnavailableException e) {
            log.debug("[{}] Request declined: failed to validate the credentials", registration.getEndpoint());
            return Authorization.declined();
        } catch (LwM2MAuthException e) {
            onAuthResult(sender, address, false);
            throw e;
        }
        onAuthResult(sender, address, authorization.isApproved());
        return authorization;
    }

    private void onAuthResult(LwM2mPeer sender, InetSocketAddress address, boolean authorized) {
        // The source address of an unsecured request can be forged: counting it would let anybody get a victim's address blocked
        if (address == null || !sender.getIdentity().isSecure()) {
            return;
        }
        if (authorized) {
            rateLimitService.onAuthSuccess(address);
        } else {
            rateLimitService.onAuthFailure(address);
        }
    }

    private Authorization authorize(Registration registration, LwM2mPeer sender) {
        SecurityInfo expectedSecurityInfo = null;
        if (securityStore != null) expectedSecurityInfo = securityStore.getByEndpoint(registration.getEndpoint());
        if (securityChecker.checkSecurityInfo(registration.getEndpoint(), sender, expectedSecurityInfo)) {
            if (sender.getIdentity() instanceof X509Identity) {
                TbX509DtlsSessionInfo sessionInfo = sessionStorage.get(registration.getEndpoint());
                if (sessionInfo != null) {
                    // X509 certificate is valid and matches endpoint.
                    clientContext.registerClient(registration, sessionInfo.getCredentials());
                }
            }
            try {
                if (expectedSecurityInfo != null && expectedSecurityInfo.usePSK() && expectedSecurityInfo.getEndpoint().equals(SecurityMode.NO_SEC.toString())
                        && expectedSecurityInfo.getPskIdentity().equals(SecurityMode.NO_SEC.toString())
                        && Arrays.equals(SecurityMode.NO_SEC.toString().getBytes(), expectedSecurityInfo.getPreSharedKey())) {
                    return Authorization.declined();
                }
            } catch (LwM2MAuthException e) {
                log.info("Registration failed: FORBIDDEN, endpointId: [{}]", registration.getEndpoint());
                return Authorization.declined();
            }
            return Authorization.approved();
        } else {
            return Authorization.declined();
        }
    }
}
