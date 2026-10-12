// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.server.client;

/**
 * The core did not answer whether the credentials exist: the client is rejected, but not counted as a client with wrong
 * credentials by the IP rate limits.
 */
public class LwM2MTransportUnavailableException extends LwM2MAuthException {

    private static final long serialVersionUID = -2468327506214633270L;

}
