// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.model.ToData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DaoUtil {

    private static final int MAX_IN_VALUE = Short.MAX_VALUE / 2;

    private static final Pattern CITUS_SHARD_SUFFIX = Pattern.compile("_(\\d+)$");

    private DaoUtil() {}

    public static <T> PageData<T> toPageData(Page<? extends ToData<T>> page) {
        List<T> data = convertDataList(page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static <T, V> PageData<V> pageToPageData(Page<T> page, Function<T, V> transform) {
        return new PageData<>(page.getContent().stream().map(transform).collect(Collectors.toList()), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static <T> PageData<T> pageToPageData(Slice<T> slice) {
        int totalPages;
        long totalElements;
        if (slice instanceof Page<T> page) {
            totalPages = page.getTotalPages();
            totalElements = page.getTotalElements();
        } else {
            totalPages = 0;
            totalElements = 0;
        }
        return new PageData<>(slice.getContent(), totalPages, totalElements, slice.hasNext());
    }

    public static Pageable toPageable(PageLink pageLink) {
        return toPageable(pageLink, true);
    }

    public static Pageable toPageable(PageLink pageLink, boolean addDefaultSorting) {
        return toPageable(pageLink, Collections.emptyMap(), addDefaultSorting);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap) {
        return toPageable(pageLink, columnMap, true);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap, boolean addDefaultSorting) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(pageLink.getSortOrder(), columnMap, addDefaultSorting));
    }

    public static Pageable toPageable(PageLink pageLink, List<SortOrder> sortOrders) {
        return toPageable(pageLink, Collections.emptyMap(), sortOrders);
    }

    public static Pageable toPageable(PageLink pageLink, String... sortColumns) {
        return toPageable(pageLink, Collections.emptyMap(), Arrays.stream(sortColumns).map(column -> new SortOrder(column, SortOrder.Direction.ASC)).toList(), false);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap, List<SortOrder> sortOrders) {
        return toPageable(pageLink, columnMap, sortOrders, true);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap, List<SortOrder> sortOrders, boolean addDefaultSorting) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(sortOrders, columnMap, addDefaultSorting));
    }

    public static <T> List<T> convertDataList(Collection<? extends ToData<T>> toConvert) {
        if (CollectionUtils.isEmpty(toConvert)) {
            return Collections.emptyList();
        }
        List<T> converted = new ArrayList<>(toConvert.size());
        for (ToData<T> object : toConvert) {
            if (object != null) {
                converted.add(object.toData());
            }
        }
        return converted;
    }

    public static <T> T getData(ToData<T> data) {
        T object = null;
        if (data != null) {
            object = data.toData();
        }
        return object;
    }

    public static <T> T getData(Optional<? extends ToData<T>> data) {
        T object = null;
        if (data.isPresent()) {
            object = data.get().toData();
        }
        return object;
    }

    public static UUID getId(UUIDBased idBased) {
        UUID id = null;
        if (idBased != null) {
            id = idBased.getId();
        }
        return id;
    }

    public static List<UUID> toUUIDs(List<? extends UUIDBased> idBasedIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUIDBased idBased : idBasedIds) {
            ids.add(getId(idBased));
        }
        return ids;
    }

    public static <I> List<I> fromUUIDs(List<UUID> uuids, Function<UUID, I> mapper) {
        return uuids.stream().map(mapper).collect(Collectors.toList());
    }

    public static <I> I toEntityId(UUID uuid, Function<UUID, I> creator) {
        if (uuid != null) {
            return creator.apply(uuid);
        } else {
            return null;
        }
    }

    public static <T> void processInBatches(Function<PageLink, PageData<T>> finder, int batchSize, Consumer<T> processor) {
        processBatches(finder, batchSize, batch -> batch.getData().forEach(processor));
    }

    public static <T> void processBatches(Function<PageLink, PageData<T>> finder, int batchSize, Consumer<PageData<T>> processor) {
        PageLink pageLink = new PageLink(batchSize);
        PageData<T> batch;

        boolean hasNextBatch;
        do {
            batch = finder.apply(pageLink);
            processor.accept(batch);

            hasNextBatch = batch.hasNext();
            pageLink = pageLink.nextPageLink();
        } while (hasNextBatch);
    }

    public static String getStringId(UUIDBased id) {
        if (id != null) {
            return id.toString();
        } else {
            return null;
        }
    }

    public static <T> ListenableFuture<List<T>> getEntitiesByTenantIdAndIdIn(List<UUID> entityIds,
                                                                             Function<List<UUID>, Collection<? extends ToData<T>>> daoConsumer,
                                                                             ListeningExecutor service) {
        int size = entityIds.size();
        List<ListenableFuture<List<T>>> resultList = new ArrayList<>();
        if (size > MAX_IN_VALUE) {
            int startIndex = 0;
            int currentSize = 0;
            while (startIndex + currentSize < size) {
                startIndex += currentSize;
                currentSize = Math.min(size - startIndex, MAX_IN_VALUE);

                List<UUID> currentEntityIds = entityIds.subList(startIndex, startIndex + currentSize);
                resultList.add(service.submit(() -> convertDataList(daoConsumer.apply(currentEntityIds))));
            }
            return Futures.transform(Futures.allAsList(resultList), list -> {
                if (!CollectionUtils.isEmpty(list)) {
                    return list.stream().flatMap(List::stream).collect(Collectors.toList());
                }

                return Collections.emptyList();
            }, service);

        } else {
            return service.submit(() -> convertDataList(daoConsumer.apply(entityIds)));
        }
    }

    public static Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

    public static List<EntitySubtype> convertTenantEntityTypesToDto(UUID tenantUUID, EntityType entityType, List<String> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }
        TenantId tenantId = TenantId.fromUUID(tenantUUID);
        return types.stream()
                .map(type -> new EntitySubtype(tenantId, entityType, type))
                .collect(Collectors.toList());
    }

    @Deprecated // used only in deprecated DAO api
    public static List<EntitySubtype> convertTenantEntityInfosToDto(UUID tenantUUID, EntityType entityType, List<EntityInfo> entityInfos) {
        if (CollectionUtils.isEmpty(entityInfos)) {
            return Collections.emptyList();
        }
        var tenantId = TenantId.fromUUID(tenantUUID);
        return entityInfos.stream()
                .map(info -> new EntitySubtype(tenantId, entityType, info.getName()))
                .sorted(Comparator.comparing(EntitySubtype::getType))
                .collect(Collectors.toList());
    }

    /**
     * Checks whether a database constraint name reported by the driver matches the expected
     * constraint name.
     * <p>
     * On a plain PostgreSQL deployment the reported name equals the declared constraint name, so a
     * direct case-insensitive comparison is sufficient. On a Citus cluster a unique/foreign-key
     * violation is raised by the shard placement on a worker node, and the driver reports the
     * constraint name with a trailing shard-id suffix (e.g. {@code rule_chain_external_id_unq_key_102011}
     * or {@code fk_asset_profile_102022}). To keep friendly-exception translation working on both
     * backends, the exact comparison is attempted first; only if it fails do we strip a trailing
     * {@code _<digits>} shard suffix and retry. Because the exact match is tried first, plain
     * PostgreSQL behavior is byte-for-byte unchanged - the strip only ever affects the Citus case
     * where the exact comparison already missed.
     *
     * @param actual   the constraint name reported by the database driver (may be {@code null})
     * @param expected the declared constraint name to match against
     * @return {@code true} if the actual name matches the expected name, ignoring case and any
     * Citus shard-placement suffix
     */
    public static boolean constraintNameMatches(String actual, String expected) {
        return actual != null
                && (actual.equalsIgnoreCase(expected) || stripCitusShardSuffix(actual).equalsIgnoreCase(expected));
    }

    /**
     * Removes a trailing Citus shard-placement suffix ({@code _<digits>}) from a constraint name.
     * Used only for constraint-name comparison; the original name is never mutated elsewhere.
     */
    private static String stripCitusShardSuffix(String constraintName) {
        return CITUS_SHARD_SUFFIX.matcher(constraintName).replaceFirst("");
    }

    public static ConstraintViolationException extractConstraintViolation(Throwable t) {
        if (t instanceof ConstraintViolationException cve) {
            return cve;
        } else if (t != null && t.getCause() instanceof ConstraintViolationException cve) {
            return cve;
        }
        return null;
    }

}
