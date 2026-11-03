// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

@Slf4j
public class PortFinder {
    public static int findAvailableUdpPort() {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        } catch (SocketException e) {
            throw new IllegalStateException("No available UDP ports found", e);
        }
    }

    public static boolean isUDPPortAvailable(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            log.debug("Failed to open UDP port {}", port, e);
            return false;
        }
    }
}
