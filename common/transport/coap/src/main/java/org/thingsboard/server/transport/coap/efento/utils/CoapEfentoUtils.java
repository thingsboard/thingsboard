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
package org.thingsboard.server.transport.coap.efento.utils;

import com.google.gson.JsonObject;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_FLOODING;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OK_ALARM;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OUTPUT_CONTROL;

public class CoapEfentoUtils {

    public static final int PULSE_CNT_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int PULSE_CNT_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int ELEC_METER_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int ELEC_METER_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int PULSE_CNT_ACC_WIDE_MINOR_METADATA_FACTOR = 6;
    public static final int PULSE_CNT_ACC_WIDE_MAJOR_METADATA_FACTOR = 4;
    public static final int WATER_METER_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int WATER_METER_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int IAQ_METADATA_FACTOR = 3;
    public static final int STATIC_IAQ_METADATA_FACTOR = 3;
    public static final int CO2_GAS_METADATA_FACTOR = 3;
    public static final int CO2_EQUIVALENT_METADATA_FACTOR = 3;
    public static final int BREATH_VOC_METADATA_FACTOR = 3;


    public static String convertByteArrayToString(byte[] a) {
        StringBuilder out = new StringBuilder();
        for (byte b : a) {
            out.append(String.format("%02X", b));
        }
        return out.toString();
    }

    public static String convertTimestampToUtcString(long timestampInMillis) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String utcZone = "UTC";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(utcZone));
        return String.format("%s UTC", simpleDateFormat.format(new Date(timestampInMillis)));
    }

    public static JsonObject setDefaultMeasurements(String serialNumber, boolean batteryStatus, long measurementPeriod, long nextTransmissionAtMillis, long signal, long startTimestampMillis) {
        JsonObject values = new JsonObject();
        values.addProperty("serial", serialNumber);
        values.addProperty("battery", batteryStatus ? "ok" : "low");
        values.addProperty("measured_at", convertTimestampToUtcString(startTimestampMillis));
        values.addProperty("next_transmission_at", convertTimestampToUtcString(nextTransmissionAtMillis));
        values.addProperty("signal", signal);
        values.addProperty("measurement_interval", measurementPeriod);
        return values;
    }

    public static boolean isBinarySensor(MeasurementType type) {
        return type == MEASUREMENT_TYPE_OK_ALARM || type == MEASUREMENT_TYPE_FLOODING || type == MEASUREMENT_TYPE_OUTPUT_CONTROL;
    }

    public static boolean isSensorError(int sampleOffset) {
        return sampleOffset >= 8355840 && sampleOffset <= 8388607;
    }

}
