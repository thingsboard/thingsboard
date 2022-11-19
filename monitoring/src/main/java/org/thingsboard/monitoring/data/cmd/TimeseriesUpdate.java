package org.thingsboard.monitoring.data.cmd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeseriesUpdate {

    private final int cmdId;
    private final int errorCode;
    private final String errorMsg;
    private Map<String, List<List<Object>>> data;

    public Object getLatest(String key) {
        if (!data.containsKey(key)) return null;
        return data.get(key).stream()
                .map(tsAndValue -> {
                    if (tsAndValue == null || tsAndValue.size() != 2) {
                        return null;
                    }
                    long ts = Long.parseLong(tsAndValue.get(0).toString());
                    Object value = tsAndValue.get(1);
                    return Pair.of(ts, value);
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Pair::getFirst))
                .map(Pair::getSecond).orElse(null);
    }

}
