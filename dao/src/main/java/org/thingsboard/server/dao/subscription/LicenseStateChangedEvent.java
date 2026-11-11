// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

/**
 * This node changed the deployment's licence state. Carries nothing: the listener's job is to tell the rest of
 * the cluster to reconverge, and every verdict then comes from the client each node rebuilds off
 * {@code tb_cluster}. A payload would be a second answer to the same question, and the only mutation it could
 * not express without one - a keyless node picking up a real licence - needs the secret, which must not travel
 * on the queue.
 * <p>
 * Published from {@code dao}, which cannot see the cluster service; the listener lives in {@code application}.
 * Published only where {@code tb_cluster} itself changes - a key applied, a licence cleared - and never from
 * the reconvergence path, which is what stops a broadcast echoing round the cluster.
 */
public class LicenseStateChangedEvent {
}
