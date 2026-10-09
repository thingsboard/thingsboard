// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataKey;

import java.util.List;

@Data
public class TsChartSeriesData {

    private TsChartDataSource dataSource;
    private DataKey dataKey;
    private List<TsChartSeriesEntry> data;
    private List<Double> numericData;
    private int index;

    private int keyIndex;

    private int datasetIndex;
    private int seriesIndex;

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    public Double calcMin() {
        if (!numericData.isEmpty()) {
            double result = numericData.get(0);
            for (int i = 1; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result = Math.min(result, value);
            }
            return result;
        } else {
            return null;
        }
    }

    public Double calcMax() {
        if (!numericData.isEmpty()) {
            double result = numericData.get(0);
            for (int i = 1; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result = Math.max(result, value);
            }
            return result;
        } else {
            return null;
        }
    }

    public Double calcTotal() {
        if (!numericData.isEmpty()) {
            double result = 0;
            for (int i = 0; i < numericData.size(); i++) {
                double value = numericData.get(i);
                result += value;
            }
            return result;
        } else {
            return null;
        }
    }

    public int calcCount() {
        return numericData.size();
    }

    public Double calcAvg() {
        Double total = calcTotal();
        if (total != null) {
            int count = calcCount();
            return total / count;
        } else {
            return null;
        }
    }

    public Double calcLatest() {
        if (!numericData.isEmpty()) {
            return numericData.get(numericData.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "TsChartSeriesData{" + "dataKey=" + dataKey +
                ", data=" + data +
                ", index=" + index +
                ", keyIndex=" + keyIndex +
                ", datasetIndex=" + datasetIndex +
                ", seriesIndex=" + seriesIndex +
                '}';
    }
}
