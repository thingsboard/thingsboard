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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/**
 * An enumeration of Sparkplug MQTT message types.  The type provides an indication as to what the MQTT Payload of 
 * message will contain.
 */
public enum SparkplugMessageType {
	
	/**
	 * Birth certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NBIRTH,
	
	/**
	 * Death certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NDEATH,
	
	/**
	 * Birth certificate for MQTT Devices.
	 */
	DBIRTH,
	
	/**
	 * Death certificate for MQTT Devices.
	 */
	DDEATH,
	
	/**
	 * Edge of Network (EoN) Node data message.
	 */
	NDATA,
	
	/**
	 * Device data message.
	 */
	DDATA,
	
	/**
	 * Edge of Network (EoN) Node command message.
	 */
	NCMD,
	
	/**
	 * Device command message.
	 */
	DCMD,
	
	/**
	 * Critical application state message.
	 */
	STATE,
	
	/**
	 * Device record message.
	 */
	DRECORD,
	
	/**
	 * Edge of Network (EoN) Node record message.
	 */
	NRECORD;
	
	public static SparkplugMessageType parseMessageType(String type) throws ThingsboardException {
		for (SparkplugMessageType messageType : SparkplugMessageType.values()) {
			if (messageType.name().equals(type)) {
				return messageType;
			}
		}
		throw new ThingsboardException("Invalid message type: " + type, ThingsboardErrorCode.INVALID_ARGUMENTS);
	}
	public static String messageName(SparkplugMessageType type) {
		return STATE.equals(type) ? "sparkplugConnectionState" : type.name();
	}
	
	public boolean isState() {
		return this.equals(STATE);
	}

	public boolean isDeath() {
		return this.equals(DDEATH) || this.equals(NDEATH);
	}

	public boolean isCommand() {
		return this.equals(DCMD) || this.equals(NCMD);
	}
	
	public boolean isData() {
		return this.equals(DDATA) || this.equals(NDATA);
	}
	
	public boolean isBirth() {
		return this.equals(DBIRTH) || this.equals(NBIRTH);
	}
	
	public boolean isRecord() {
		return this.equals(DRECORD) || this.equals(NRECORD);
	}
	public boolean isSubscribe() {
		return isCommand() || isData() || isRecord();
	}

	public boolean isNode() {
		return this.equals(NBIRTH)
				|| this.equals(NCMD) || this.equals(NDATA)
				||this.equals(NDEATH) || this.equals(NRECORD);
	}
	public boolean isDevice() {
		return this.equals(DBIRTH)
				|| this.equals(DCMD) || this.equals(DDATA)
				||this.equals(DDEATH) || this.equals(DRECORD);
	}

}
