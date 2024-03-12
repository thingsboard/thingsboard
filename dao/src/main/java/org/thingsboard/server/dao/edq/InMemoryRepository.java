package org.thingsboard.server.dao.edq;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemoryRepository {

    private final static ConcurrentSkipListSet<DeviceRepoData> devices = new ConcurrentSkipListSet<>(Comparator.comparingLong(o -> o.getDevice().getCreatedTime()));
    private final static ConcurrentMap<DeviceId, DeviceRepoData> devicesById = new ConcurrentHashMap<>();

    public void add(Device device) {
        var dd = new DeviceRepoData(device);
        devicesById.put(dd.getDevice().getId(), dd);
        devices.add(dd);
    }

    public void add(DeviceId deviceId, Integer attrKey, String attrValue) {
        devicesById.get(deviceId).putAttr(attrKey, attrValue);
    }

    public List<DeviceRepoData> findDevices(int page, int pageSize, Predicate<DeviceRepoData> predicate, Comparator<DeviceRepoData> comparator) {
        var stream = devices.stream();
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        return stream.skip(page * pageSize).limit(pageSize).collect(Collectors.toList());
    }

    public List<DeviceRepoData> findDevices(int page, int pageSize, Predicate<DeviceRepoData> predicate, Function<DeviceRepoData, String> sortFieldFunction, Comparator<SortPair> comparator) {
        var stream = devices.stream();
        if (predicate != null) {
            stream = stream.filter(predicate);
        }

        if (comparator != null) {
            var sortedStream = stream.map(d -> new SortPair(sortFieldFunction.apply(d), d));
            sortedStream = sortedStream.sorted(comparator);
            return sortedStream.skip(page * pageSize).limit(pageSize).map(v -> v.data).collect(Collectors.toList());
        } else {
            return stream.skip(page * pageSize).limit(pageSize).collect(Collectors.toList());
        }
    }
}
