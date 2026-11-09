// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusRtuClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.digitalpetri.modbus.tcp.client.NettyClientTransportConfig;
import com.digitalpetri.modbus.tcp.client.NettyRtuClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import io.netty.channel.EventLoopGroup;
import lombok.Builder;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * {@link ModbusClientAdapter} over digitalpetri/modbus: {@link ModbusTcpClient} for MBAP framing,
 * {@link ModbusRtuClient} for RTU frames tunnelled over TCP. Reconnects are handled by the library transport.
 */
public class DigitalPetriModbusClientAdapter implements ModbusClientAdapter {

    private final ModbusClient client;

    @Builder
    public DigitalPetriModbusClientAdapter(String host, int port, ModbusTcpFraming framing, Duration requestTimeout,
                                           Duration connectTimeout, EventLoopGroup eventLoopGroup, ExecutorService executor) {
        Consumer<NettyClientTransportConfig.Builder> transportConfig = cfg -> {
            cfg.hostname = host;
            cfg.port = port;
            cfg.connectTimeout = connectTimeout;
            cfg.connectPersistent = true;
            cfg.eventLoopGroup = eventLoopGroup;
            cfg.executor = executor;
        };
        this.client = switch (framing) {
            case MBAP -> ModbusTcpClient.create(NettyTcpClientTransport.create(transportConfig), cfg -> cfg.requestTimeout = requestTimeout);
            case RTU -> ModbusRtuClient.create(NettyRtuClientTransport.create(transportConfig), cfg -> cfg.requestTimeout = requestTimeout);
        };
    }

    @Override
    public CompletionStage<Void> connect() {
        return client.connectAsync();
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return client.disconnectAsync();
    }

    @Override
    public CompletionStage<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count) {
        return switch (functionCode) {
            case READ_COILS -> client.readCoilsAsync(unitId, new ReadCoilsRequest(address, count)).thenApply(ReadCoilsResponse::coils);
            case READ_DISCRETE_INPUTS -> client.readDiscreteInputsAsync(unitId, new ReadDiscreteInputsRequest(address, count))
                    .thenApply(ReadDiscreteInputsResponse::inputs);
            case READ_HOLDING_REGISTERS -> client.readHoldingRegistersAsync(unitId, new ReadHoldingRegistersRequest(address, count))
                    .thenApply(ReadHoldingRegistersResponse::registers);
            case READ_INPUT_REGISTERS -> client.readInputRegistersAsync(unitId, new ReadInputRegistersRequest(address, count))
                    .thenApply(ReadInputRegistersResponse::registers);
            default -> throw new IllegalArgumentException("Function code " + functionCode.getCode() + " is not a read function code");
        };
    }

}
