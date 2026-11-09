// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.service.TransportActivityManager;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.modbus.ModbusTransportContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session of one Modbus device owned by this node.
 * <p>
 * The object itself is the claim in {@link ModbusTransportContext}'s session map: it is put there in {@link State#ESTABLISHING}
 * before the token is validated, becomes {@link State#ACTIVE} once registered with core, and {@link State#CLOSED} when destroyed.
 * State transitions are synchronized on the session, so establishing and destroying cannot interleave.
 */
@Slf4j
public class ModbusSessionContext extends DeviceAwareSessionContext implements SessionMsgListener {

    public enum State {ESTABLISHING, ACTIVE, CLOSED}

    private final ModbusTransportContext parentCtx;
    private final AtomicInteger msgIdSeq = new AtomicInteger(0);
    @Getter
    private final List<ScheduledFuture<?>> pollTasks = new CopyOnWriteArrayList<>();

    @Getter
    private volatile State state = State.ESTABLISHING;
    @Getter
    @Setter
    private volatile Device device;
    @Getter
    @Setter
    private volatile ModbusDeviceTransportConfiguration deviceConfig;
    @Getter
    @Setter
    private volatile ModbusDeviceProfileTransportConfiguration profileConfig;
    @Setter
    private volatile Runnable sessionTimeoutHandler;

    public ModbusSessionContext(ModbusTransportContext parentCtx, Device device, ModbusDeviceTransportConfiguration deviceConfig) {
        super(UUID.randomUUID());
        this.parentCtx = parentCtx;
        this.device = device;
        this.deviceConfig = deviceConfig;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    /**
     * Runs {@code onActivate} and moves to {@link State#ACTIVE}, but only if the session is still establishing.
     *
     * @return {@code false} if the session was closed meanwhile; {@code onActivate} is then not run
     */
    public synchronized boolean activate(Runnable onActivate) {
        if (state != State.ESTABLISHING) {
            return false;
        }
        onActivate.run();
        state = State.ACTIVE;
        return true;
    }

    /**
     * Moves to {@link State#CLOSED} and cancels poll tasks.
     *
     * @return {@code true} if the session was active, i.e. registered with core and in need of deregistration
     */
    public synchronized boolean close() {
        boolean wasActive = state == State.ACTIVE;
        state = State.CLOSED;
        pollTasks.forEach(task -> task.cancel(false));
        pollTasks.clear();
        setDisconnected();
        return wasActive;
    }

    @Override
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    @Override
    public void onDeviceProfileUpdate(SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(newSessionInfo, deviceProfile);
        if (isActive()) {
            parentCtx.onDeviceProfileUpdated(this, deviceProfile);
        }
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        parentCtx.onDeviceDeleted(deviceId);
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        if (TransportActivityManager.SESSION_EXPIRED_MESSAGE.equals(sessionCloseNotification.getMessage())) {
            Runnable handler = sessionTimeoutHandler;
            if (handler != null && isActive()) {
                handler.run();
            }
        }
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg attributeUpdateNotification) {
        // todo: write mapped shared attributes (attributeUpdates)
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg toDeviceRequest) {
        // todo: read/write by rpc mapping; reply with ToDeviceRpcResponseMsg (payload or error)
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
    }

}
