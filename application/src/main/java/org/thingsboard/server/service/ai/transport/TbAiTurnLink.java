// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

/**
 * Connects the browser end of an AI chat stream to its turn on TB AI. ThingsBoard keeps listening to a turn after the
 * browser leaves (the turn bills its credits at its end), so leaving is reported separately: TB AI then knows nobody
 * follows the turn, and a pending approval expires soon instead of waiting its full timeout for a user who left.
 */
public final class TbAiTurnLink {

    private final Object lock = new Object();
    private Runnable onClientLeft;
    private boolean clientLeft;

    public void onClientLeft(Runnable listener) {
        boolean leftAlready;
        synchronized (lock) {
            onClientLeft = listener;
            leftAlready = clientLeft;
        }
        if (leftAlready) {
            listener.run();
        }
    }

    public void clientLeft() {
        Runnable listener;
        synchronized (lock) {
            if (clientLeft) {
                return;
            }
            clientLeft = true;
            listener = onClientLeft;
        }
        if (listener != null) {
            listener.run();
        }
    }

}
