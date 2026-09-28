// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import java.math.BigInteger;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public enum OpcUaType {

    INT8("Int8", "SByte") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).byteValue();
        }
    },
    UINT8("UInt8", "Byte") {
        @Override
        public Object convertValue(Object raw) {
            return ubyte(((Number) raw).intValue());
        }
    },
    INT16("Int16") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).shortValue();
        }
    },
    UINT16("UInt16") {
        @Override
        public Object convertValue(Object raw) {
            return ushort(((Number) raw).intValue());
        }
    },
    INT32("Int32") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).intValue();
        }
    },
    UINT32("UInt32") {
        @Override
        public Object convertValue(Object raw) {
            return uint(((Number) raw).longValue());
        }
    },
    INT64("Int64") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).longValue();
        }
    },
    UINT64("UInt64") {
        @Override
        public Object convertValue(Object raw) {
            return ulong((BigInteger) (raw));
        }
    },

    FLOAT("Float") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).floatValue();
        }
    },
    DOUBLE("Double") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).doubleValue();
        }
    },

    BOOLEAN("Boolean") {
        @Override
        public Object convertValue(Object raw) {
            return raw;
        }
    },

    STRING("String") {
        @Override
        public Object convertValue(Object raw) {
            return String.valueOf(raw);
        }
    };

    private final String[] typeAliases;

    OpcUaType(String... aliases) {
        this.typeAliases = aliases;
    }

    public abstract Object convertValue(Object raw);

    public static OpcUaType fromOpcUaType(String name) {
        for (OpcUaType t : values())
            for (String a : t.typeAliases)
                if (a.equals(name))
                    return t;

        return STRING;
    }

}
