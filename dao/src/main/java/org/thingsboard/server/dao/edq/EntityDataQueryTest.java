package org.thingsboard.server.dao.edq;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public class EntityDataQueryTest {

    private static final int DEVICE_COUNT = 500000;
    private static final int ATTR_COUNT = 100;
    private static final Comparator<DeviceRepoData> CREATED_TIME_COMPARATOR = Comparator.comparingLong(o -> o.getDevice().getCreatedTime());

    private static final Comparator<DeviceRepoData> ATTR_COMPARATOR = Comparator.comparing(o -> o.getAttrs().get(10));

    private static final Comparator<SortPair> SORT_ASC = Comparator.comparing(SortPair::getKey);
    private static final Comparator<SortPair> SORT_DESC = Comparator.comparing(SortPair::getKey).reversed();

    public static void main(String[] args) {
        InMemoryRepository repository = new InMemoryRepository();
        long startTs = System.currentTimeMillis();
        long ts = System.currentTimeMillis() - DEVICE_COUNT;
        for (int i = 0; i < DEVICE_COUNT; i++) {
            DeviceId deviceId = new DeviceId(UUID.randomUUID());
            Device device = new Device();
            device.setId(deviceId);
            device.setCreatedTime(ts + i);
            device.setName("Device " + i);
            device.setLabel("Device Label" + i);
            device.setType("Device Type " + (i % 100));
            repository.add(device);
            String random = StringUtils.randomAlphanumeric(5);
            for (int j = 0; j < ATTR_COUNT; j++) {
                repository.add(deviceId, j, random);
            }
        }
        log.error("Repository created in {}", System.currentTimeMillis() - startTs);


        for (int i = 0; i < 1; i++) {
            test("DeviceName filter, sort by attribute desc", repository, 10, d -> d.getDevice().getName().startsWith("Device 9"), ATTR_COMPARATOR.reversed());
            test2("DeviceName filter, sort by attribute desc", repository, 10, d -> d.getDevice().getName().startsWith("Device 8"), d -> d.getAttrs().get(10), SORT_DESC);

            test("DeviceType filter, no sort", repository, 3, d -> d.getDevice().getType().equals("Device Type 10"), null);
            test2("DeviceType filter, no sort", repository, 3, d -> d.getDevice().getType().equals("Device Type 10"), null, null);

            test("Attribute filter, no sort", repository, 3, d -> d.getAttrs().get(10).contains("1"), null);
            test2("Attribute filter, no sort", repository, 3, d -> d.getAttrs().get(9).contains("2"), null, null);

            test("No filter, sort by attribute", repository, 3, null, ATTR_COMPARATOR);
            test2("No filter, sort by attribute", repository, 3, null, d -> d.getAttrs().get(10), SORT_ASC);

            test("Attribute filter, createdTime", repository, 3, d -> d.getAttrs().get(10).contains("1"), CREATED_TIME_COMPARATOR);
            test("No filter, sort by createdTime", repository, 3, null, CREATED_TIME_COMPARATOR);
        }
    }

    private static void test(String testName, InMemoryRepository repository, int pages, Predicate<DeviceRepoData> predicate, Comparator<DeviceRepoData> comparator) {
        log.error("===================================================================================================");
        log.error("============================ 1 = {} =======================================", testName);
        List<Long> execTimes = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            long startTs = System.nanoTime();
            var results = repository.findDevices(i, 10, predicate, comparator);
            if (results.isEmpty()) {
                log.error("Empty results");
            }
            long endTs = System.nanoTime();
            execTimes.add(endTs - startTs);
        }
        log.error("Run queries in {}", execTimes);
        log.error("Average {}", execTimes.stream().mapToDouble(v -> (double) v).average().orElse(0.0) / 1000000.0);
        log.error("===================================================================================================");
    }

    private static void test2(String testName, InMemoryRepository repository, int pages, Predicate<DeviceRepoData> predicate, Function<DeviceRepoData, String> toSortKey, Comparator<SortPair> comparator) {
        log.error("===================================================================================================");
        log.error("=========================== 2 = {} =======================================", testName);
        List<Long> execTimes = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            long startTs = System.nanoTime();
            var results = repository.findDevices(i, 10, predicate, toSortKey, comparator);
            if (results.isEmpty()) {
                log.error("Empty results");
            }
            long endTs = System.nanoTime();
            execTimes.add(endTs - startTs);
        }
        log.error("Run queries in {}", execTimes);
        log.error("Average {}", execTimes.stream().mapToDouble(v -> (double) v).average().orElse(0.0) / 1000000.0);
        log.error("===================================================================================================");
    }


}
