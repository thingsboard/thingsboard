/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa;

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
