package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.db.JooqTime;
import com.iot.thingmodel.db.JsonbCodec;
import com.iot.thingmodel.tsl.TslAction;
import com.iot.thingmodel.tsl.TslDefinition;
import com.iot.thingmodel.tsl.TslEvent;
import com.iot.thingmodel.tsl.TslProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.iot.thingmodel.db.jooq.Tables.PLAT_PRODUCT;
import static com.iot.thingmodel.db.jooq.Tables.PLAT_PRODUCT_TSL;
import static com.iot.thingmodel.db.jooq.Tables.PLAT_PROFILE_BINDING;
import static com.iot.thingmodel.db.jooq.Tables.PLAT_TSL_ACTION;
import static com.iot.thingmodel.db.jooq.Tables.PLAT_TSL_EVENT;
import static com.iot.thingmodel.db.jooq.Tables.PLAT_TSL_PROPERTY;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectCount;
import static org.jooq.impl.DSL.trueCondition;

@Repository
public class ProductRepository {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public ProductRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public Optional<ProductRow> findProduct(String productId) {
        return dsl.selectFrom(PLAT_PRODUCT)
                .where(PLAT_PRODUCT.PRODUCT_ID.eq(productId))
                .fetchOptional(this::mapProduct);
    }

    public List<ProductItem> listProducts(UUID tenantId, String keyword) {
        var latest = dsl.select(PLAT_PRODUCT_TSL.asterisk())
                .distinctOn(PLAT_PRODUCT_TSL.PRODUCT_ID)
                .from(PLAT_PRODUCT_TSL)
                .orderBy(
                        PLAT_PRODUCT_TSL.PRODUCT_ID,
                        PLAT_PRODUCT_TSL.CREATED_AT.desc(),
                        PLAT_PRODUCT_TSL.ID.desc())
                .asTable(Constants.Db.LATEST_ALIAS);

        Field<String> latestProductId = latest.field(PLAT_PRODUCT_TSL.PRODUCT_ID);
        Field<String> latestVersion = latest.field(PLAT_PRODUCT_TSL.VERSION);
        Field<String> tslVersion = latestVersion.as(Constants.Db.ALIAS_TSL_VERSION);
        Field<String> tslStatus = latest.field(PLAT_PRODUCT_TSL.STATUS)
                .as(Constants.Db.ALIAS_TSL_STATUS);
        Field<Integer> propertyCount = childCount(
                        PLAT_TSL_PROPERTY,
                        PLAT_TSL_PROPERTY.PRODUCT_ID,
                        PLAT_TSL_PROPERTY.TSL_VERSION,
                        latestVersion)
                .as(Constants.Db.ALIAS_PROPERTY_COUNT);
        Field<Integer> eventCount = childCount(
                        PLAT_TSL_EVENT,
                        PLAT_TSL_EVENT.PRODUCT_ID,
                        PLAT_TSL_EVENT.TSL_VERSION,
                        latestVersion)
                .as(Constants.Db.ALIAS_EVENT_COUNT);
        Field<Integer> actionCount = childCount(
                        PLAT_TSL_ACTION,
                        PLAT_TSL_ACTION.PRODUCT_ID,
                        PLAT_TSL_ACTION.TSL_VERSION,
                        latestVersion)
                .as(Constants.Db.ALIAS_ACTION_COUNT);

        Condition condition = trueCondition();
        if (tenantId != null) {
            condition = condition.and(PLAT_PRODUCT.TENANT_ID.eq(tenantId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = String.format(Constants.Db.LIKE_PATTERN, keyword.trim());
            condition = condition.and(PLAT_PRODUCT.PRODUCT_ID.likeIgnoreCase(pattern)
                    .or(PLAT_PRODUCT.NAME.likeIgnoreCase(pattern)));
        }

        return dsl.select(
                        PLAT_PRODUCT.TENANT_ID,
                        PLAT_PRODUCT.PRODUCT_ID,
                        PLAT_PRODUCT.NAME,
                        PLAT_PRODUCT.MODEL,
                        PLAT_PRODUCT.CATEGORY_ID,
                        tslVersion,
                        tslStatus,
                        propertyCount,
                        eventCount,
                        actionCount,
                        PLAT_PROFILE_BINDING.TB_PROFILE_ID,
                        PLAT_PROFILE_BINDING.TB_ASSET_ID,
                        PLAT_PRODUCT.UPDATED_AT)
                .from(PLAT_PRODUCT)
                .leftJoin(latest).on(latestProductId.eq(PLAT_PRODUCT.PRODUCT_ID))
                .leftJoin(PLAT_PROFILE_BINDING).on(
                        PLAT_PROFILE_BINDING.PRODUCT_ID.eq(PLAT_PRODUCT.PRODUCT_ID)
                                .and(PLAT_PROFILE_BINDING.TSL_VERSION.eq(latestVersion)))
                .where(condition)
                .orderBy(PLAT_PRODUCT.UPDATED_AT.desc(), PLAT_PRODUCT.PRODUCT_ID.asc())
                .fetch(record -> new ProductItem(
                        record.get(PLAT_PRODUCT.TENANT_ID).toString(),
                        record.get(PLAT_PRODUCT.PRODUCT_ID),
                        record.get(PLAT_PRODUCT.NAME),
                        record.get(PLAT_PRODUCT.MODEL),
                        record.get(PLAT_PRODUCT.CATEGORY_ID),
                        record.get(tslVersion),
                        record.get(tslStatus),
                        longValue(record.get(propertyCount)),
                        longValue(record.get(eventCount)),
                        longValue(record.get(actionCount)),
                        uuidString(record.get(PLAT_PROFILE_BINDING.TB_PROFILE_ID)),
                        uuidString(record.get(PLAT_PROFILE_BINDING.TB_ASSET_ID)),
                        JooqTime.toInstant(record.get(PLAT_PRODUCT.UPDATED_AT))));
    }

    public Optional<TslRow> findTsl(String productId, String version) {
        return dsl.selectFrom(PLAT_PRODUCT_TSL)
                .where(PLAT_PRODUCT_TSL.PRODUCT_ID.eq(productId))
                .and(PLAT_PRODUCT_TSL.VERSION.eq(version))
                .fetchOptional(this::mapTsl);
    }

    public Optional<TslRow> findLatestTsl(String productId) {
        return dsl.selectFrom(PLAT_PRODUCT_TSL)
                .where(PLAT_PRODUCT_TSL.PRODUCT_ID.eq(productId))
                .orderBy(PLAT_PRODUCT_TSL.CREATED_AT.desc(), PLAT_PRODUCT_TSL.ID.desc())
                .limit(Constants.Db.LATEST_LIMIT)
                .fetchOptional(this::mapTsl);
    }

    public List<ProductDetail.TslVersionView> listVersions(String productId) {
        return dsl.select(
                        PLAT_PRODUCT_TSL.VERSION,
                        PLAT_PRODUCT_TSL.STATUS,
                        PLAT_PRODUCT_TSL.CHECKSUM,
                        PLAT_PRODUCT_TSL.PUBLISHED_AT,
                        PLAT_PRODUCT_TSL.CREATED_AT)
                .from(PLAT_PRODUCT_TSL)
                .where(PLAT_PRODUCT_TSL.PRODUCT_ID.eq(productId))
                .orderBy(PLAT_PRODUCT_TSL.CREATED_AT.desc(), PLAT_PRODUCT_TSL.ID.desc())
                .fetch(record -> new ProductDetail.TslVersionView(
                        record.get(PLAT_PRODUCT_TSL.VERSION),
                        record.get(PLAT_PRODUCT_TSL.STATUS),
                        record.get(PLAT_PRODUCT_TSL.CHECKSUM),
                        JooqTime.toInstant(record.get(PLAT_PRODUCT_TSL.PUBLISHED_AT)),
                        JooqTime.toInstant(record.get(PLAT_PRODUCT_TSL.CREATED_AT))));
    }

    public Optional<BindingRow> findBinding(String productId, String version) {
        return dsl.select(
                        PLAT_PROFILE_BINDING.TB_PROFILE_ID,
                        PLAT_PROFILE_BINDING.TB_ASSET_ID,
                        PLAT_PROFILE_BINDING.TB_RULE_CHAIN_ID,
                        PLAT_PROFILE_BINDING.SCHEMA_VERSION,
                        PLAT_PROFILE_BINDING.SYNC_RESULT,
                        PLAT_PROFILE_BINDING.SYNCED_AT)
                .from(PLAT_PROFILE_BINDING)
                .where(PLAT_PROFILE_BINDING.PRODUCT_ID.eq(productId))
                .and(PLAT_PROFILE_BINDING.TSL_VERSION.eq(version))
                .fetchOptional(record -> new BindingRow(
                        record.get(PLAT_PROFILE_BINDING.TB_PROFILE_ID),
                        record.get(PLAT_PROFILE_BINDING.TB_ASSET_ID),
                        record.get(PLAT_PROFILE_BINDING.TB_RULE_CHAIN_ID),
                        record.get(PLAT_PROFILE_BINDING.SCHEMA_VERSION),
                        record.get(PLAT_PROFILE_BINDING.SYNC_RESULT),
                        JooqTime.toInstant(record.get(PLAT_PROFILE_BINDING.SYNCED_AT))));
    }

    @Transactional
    public DraftResult saveDraft(
            UUID tenantId,
            String productId,
            String name,
            String model,
            String categoryId,
            String vendor,
            String version,
            String checksum,
            boolean strictUnknown,
            JsonNode tsl) {
        ProductRow existing = findProductForUpdate(productId);
        if (existing != null && !existing.tenantId().equals(tenantId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    Constants.ErrorCode.PRODUCT_ID_CONFLICT,
                    "ProductId 已被其他厂商占用");
        }

        try {
            if (existing == null) {
                dsl.insertInto(PLAT_PRODUCT)
                        .set(PLAT_PRODUCT.TENANT_ID, tenantId)
                        .set(PLAT_PRODUCT.PRODUCT_ID, productId)
                        .set(PLAT_PRODUCT.NAME, name)
                        .set(PLAT_PRODUCT.MODEL, model)
                        .set(PLAT_PRODUCT.CATEGORY_ID, categoryId)
                        .set(PLAT_PRODUCT.VENDOR, vendor)
                        .set(PLAT_PRODUCT.AUTH_TYPE, Constants.Product.AUTH_TYPE_ACCESS_TOKEN)
                        .set(PLAT_PRODUCT.PROTOCOL, Constants.Product.PROTOCOL_MQTT)
                        .execute();
            } else {
                dsl.update(PLAT_PRODUCT)
                        .set(PLAT_PRODUCT.NAME, name)
                        .set(PLAT_PRODUCT.MODEL, model)
                        .set(PLAT_PRODUCT.CATEGORY_ID, categoryId)
                        .set(PLAT_PRODUCT.VENDOR, vendor)
                        .where(PLAT_PRODUCT.PRODUCT_ID.eq(productId))
                        .execute();
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT, Constants.ErrorCode.PRODUCT_CONFLICT, "产品创建冲突，请重试");
        }

        TslRow existingTsl = findTslForUpdate(productId, version);
        if (existingTsl != null) {
            if (!existingTsl.checksum().equals(checksum)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        Constants.ErrorCode.TSL_VERSION_EXISTS,
                        "同一 TSL 版本已存在且内容不同");
            }
            ProductRow product = findProduct(productId).orElseThrow();
            return new DraftResult(product, existingTsl);
        }

        dsl.insertInto(PLAT_PRODUCT_TSL)
                .set(PLAT_PRODUCT_TSL.TENANT_ID, tenantId)
                .set(PLAT_PRODUCT_TSL.PRODUCT_ID, productId)
                .set(PLAT_PRODUCT_TSL.VERSION, version)
                .set(PLAT_PRODUCT_TSL.CHECKSUM, checksum)
                .set(PLAT_PRODUCT_TSL.STRICT_UNKNOWN, strictUnknown)
                .set(PLAT_PRODUCT_TSL.TSL_JSON, JsonbCodec.toJsonb(objectMapper, tsl))
                .set(PLAT_PRODUCT_TSL.STATUS, Constants.Product.STATUS_DRAFT)
                .execute();

        ProductRow product = findProduct(productId).orElseThrow();
        TslRow tslRow = findTsl(productId, version).orElseThrow();
        return new DraftResult(product, tslRow);
    }

    @Transactional
    public void publish(
            String productId,
            String version,
            TslDefinition definition,
            String schemaVersion,
            UUID profileId,
            UUID assetId,
            UUID ruleChainId,
            String syncResult) {
        int updated = dsl.update(PLAT_PRODUCT_TSL)
                .set(PLAT_PRODUCT_TSL.STATUS, Constants.Product.STATUS_PUBLISHED)
                .set(PLAT_PRODUCT_TSL.PUBLISHED_AT, JooqTime.now())
                .where(PLAT_PRODUCT_TSL.PRODUCT_ID.eq(productId))
                .and(PLAT_PRODUCT_TSL.VERSION.eq(version))
                .execute();
        if (updated == Constants.Db.NO_ROWS_AFFECTED) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, Constants.ErrorCode.TSL_NOT_FOUND, "物模型版本不存在");
        }

        dsl.deleteFrom(PLAT_TSL_PROPERTY)
                .where(PLAT_TSL_PROPERTY.PRODUCT_ID.eq(productId))
                .and(PLAT_TSL_PROPERTY.TSL_VERSION.eq(version))
                .execute();
        dsl.deleteFrom(PLAT_TSL_EVENT)
                .where(PLAT_TSL_EVENT.PRODUCT_ID.eq(productId))
                .and(PLAT_TSL_EVENT.TSL_VERSION.eq(version))
                .execute();
        dsl.deleteFrom(PLAT_TSL_ACTION)
                .where(PLAT_TSL_ACTION.PRODUCT_ID.eq(productId))
                .and(PLAT_TSL_ACTION.TSL_VERSION.eq(version))
                .execute();

        insertProperties(productId, version, definition.properties());
        insertEvents(productId, version, definition.events());
        insertActions(productId, version, definition.actions());

        dsl.insertInto(PLAT_PROFILE_BINDING)
                .set(PLAT_PROFILE_BINDING.PRODUCT_ID, productId)
                .set(PLAT_PROFILE_BINDING.TSL_VERSION, version)
                .set(PLAT_PROFILE_BINDING.TB_PROFILE_ID, profileId)
                .set(PLAT_PROFILE_BINDING.TB_ASSET_ID, assetId)
                .set(PLAT_PROFILE_BINDING.TB_RULE_CHAIN_ID, ruleChainId)
                .set(PLAT_PROFILE_BINDING.SCHEMA_VERSION, schemaVersion)
                .set(PLAT_PROFILE_BINDING.SYNC_RESULT, syncResult)
                .set(PLAT_PROFILE_BINDING.SYNCED_AT, JooqTime.now())
                .onConflict(PLAT_PROFILE_BINDING.PRODUCT_ID, PLAT_PROFILE_BINDING.TSL_VERSION)
                .doUpdate()
                .set(PLAT_PROFILE_BINDING.TB_PROFILE_ID, excluded(PLAT_PROFILE_BINDING.TB_PROFILE_ID))
                .set(PLAT_PROFILE_BINDING.TB_ASSET_ID, excluded(PLAT_PROFILE_BINDING.TB_ASSET_ID))
                .set(PLAT_PROFILE_BINDING.TB_RULE_CHAIN_ID, excluded(PLAT_PROFILE_BINDING.TB_RULE_CHAIN_ID))
                .set(PLAT_PROFILE_BINDING.SCHEMA_VERSION, excluded(PLAT_PROFILE_BINDING.SCHEMA_VERSION))
                .set(PLAT_PROFILE_BINDING.SYNC_RESULT, excluded(PLAT_PROFILE_BINDING.SYNC_RESULT))
                .set(PLAT_PROFILE_BINDING.SYNCED_AT, JooqTime.now())
                .execute();

        dsl.update(PLAT_PRODUCT)
                .set(PLAT_PRODUCT.UPDATED_AT, JooqTime.now())
                .where(PLAT_PRODUCT.PRODUCT_ID.eq(productId))
                .execute();
    }

    private void insertProperties(String productId, String version, List<TslProperty> properties) {
        if (properties.isEmpty()) {
            return;
        }
        List<Query> queries = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            TslProperty property = properties.get(i);
            queries.add(dsl.insertInto(PLAT_TSL_PROPERTY)
                    .set(PLAT_TSL_PROPERTY.PRODUCT_ID, productId)
                    .set(PLAT_TSL_PROPERTY.TSL_VERSION, version)
                    .set(PLAT_TSL_PROPERTY.PROPERTY_KEY, property.id())
                    .set(PLAT_TSL_PROPERTY.NAME, property.name())
                    .set(PLAT_TSL_PROPERTY.DATA_TYPE, property.define() == null ? null : property.define().type())
                    .set(PLAT_TSL_PROPERTY.MODE, property.mode())
                    .set(PLAT_TSL_PROPERTY.UNIT, property.define() == null ? null : property.define().unit())
                    .set(PLAT_TSL_PROPERTY.MIN_VALUE, property.define() == null ? null : property.define().min())
                    .set(PLAT_TSL_PROPERTY.MAX_VALUE, property.define() == null ? null : property.define().max())
                    .set(PLAT_TSL_PROPERTY.STEP_VALUE, property.define() == null ? null : property.define().step())
                    .set(PLAT_TSL_PROPERTY.REQUIRED, property.required())
                    .set(PLAT_TSL_PROPERTY.MAPPING_JSON, JsonbCodec.toJsonb(objectMapper, property.mapping()))
                    .set(PLAT_TSL_PROPERTY.COLUMN_NAME, columnName(property.id()))
                    .set(PLAT_TSL_PROPERTY.SORT_ORDER, i));
        }
        dsl.batch(queries).execute();
    }

    private void insertEvents(String productId, String version, List<TslEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        List<Query> queries = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            TslEvent event = events.get(i);
            queries.add(dsl.insertInto(PLAT_TSL_EVENT)
                    .set(PLAT_TSL_EVENT.PRODUCT_ID, productId)
                    .set(PLAT_TSL_EVENT.TSL_VERSION, version)
                    .set(PLAT_TSL_EVENT.EVENT_ID, event.id())
                    .set(PLAT_TSL_EVENT.NAME, event.name())
                    .set(PLAT_TSL_EVENT.EVENT_TYPE, event.type())
                    .set(PLAT_TSL_EVENT.PARAMS_JSON, JsonbCodec.toJsonb(objectMapper, event.params()))
                    .set(PLAT_TSL_EVENT.SORT_ORDER, i));
        }
        dsl.batch(queries).execute();
    }

    private void insertActions(String productId, String version, List<TslAction> actions) {
        if (actions.isEmpty()) {
            return;
        }
        List<Query> queries = new ArrayList<>(actions.size());
        for (int i = 0; i < actions.size(); i++) {
            TslAction action = actions.get(i);
            queries.add(dsl.insertInto(PLAT_TSL_ACTION)
                    .set(PLAT_TSL_ACTION.PRODUCT_ID, productId)
                    .set(PLAT_TSL_ACTION.TSL_VERSION, version)
                    .set(PLAT_TSL_ACTION.ACTION_ID, action.id())
                    .set(PLAT_TSL_ACTION.NAME, action.name())
                    .set(PLAT_TSL_ACTION.INPUT_JSON, JsonbCodec.toJsonb(objectMapper, action.input()))
                    .set(PLAT_TSL_ACTION.OUTPUT_JSON, JsonbCodec.toJsonb(objectMapper, action.output()))
                    .set(PLAT_TSL_ACTION.SORT_ORDER, i));
        }
        dsl.batch(queries).execute();
    }

    private ProductRow findProductForUpdate(String productId) {
        return dsl.selectFrom(PLAT_PRODUCT)
                .where(PLAT_PRODUCT.PRODUCT_ID.eq(productId))
                .forUpdate()
                .fetchOne(this::mapProduct);
    }

    private TslRow findTslForUpdate(String productId, String version) {
        return dsl.selectFrom(PLAT_PRODUCT_TSL)
                .where(PLAT_PRODUCT_TSL.PRODUCT_ID.eq(productId))
                .and(PLAT_PRODUCT_TSL.VERSION.eq(version))
                .forUpdate()
                .fetchOne(this::mapTsl);
    }

    private ProductRow mapProduct(com.iot.thingmodel.db.jooq.tables.records.PlatProductRecord record) {
        return new ProductRow(
                record.getTenantId(),
                record.getProductId(),
                record.getName(),
                record.getModel(),
                record.getCategoryId(),
                record.getVendor(),
                record.getAuthType(),
                record.getProtocol(),
                JooqTime.toInstant(record.getCreatedAt()),
                JooqTime.toInstant(record.getUpdatedAt()));
    }

    private TslRow mapTsl(com.iot.thingmodel.db.jooq.tables.records.PlatProductTslRecord record) {
        return new TslRow(
                record.getId(),
                record.getTenantId(),
                record.getProductId(),
                record.getVersion(),
                record.getChecksum(),
                JsonbCodec.toJsonNode(objectMapper, record.getTslJson()),
                Boolean.TRUE.equals(record.getStrictUnknown()),
                record.getStatus(),
                JooqTime.toInstant(record.getPublishedAt()),
                JooqTime.toInstant(record.getCreatedAt()));
    }

    private Field<Integer> childCount(
            Table<?> child,
            Field<String> childProductId,
            Field<String> childTslVersion,
            Field<String> version) {
        return DSL.coalesce(
                        field(selectCount()
                                .from(child)
                                .where(childProductId.eq(PLAT_PRODUCT.PRODUCT_ID))
                                .and(childTslVersion.eq(version))),
                        DSL.inline(Constants.Db.NO_ROWS_AFFECTED))
                .cast(Integer.class);
    }

    private long longValue(Integer value) {
        return value == null ? Constants.Db.ZERO_COUNT : value.longValue();
    }

    private String uuidString(UUID value) {
        return value == null ? null : value.toString();
    }

    private String columnName(String propertyId) {
        return Constants.Product.COLUMN_TS.equals(propertyId)
                ? Constants.Product.COLUMN_TS_V
                : propertyId;
    }

    public record ProductRow(
            UUID tenantId,
            String productId,
            String name,
            String model,
            String categoryId,
            String vendor,
            String authType,
            String protocol,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record TslRow(
            long id,
            UUID tenantId,
            String productId,
            String version,
            String checksum,
            JsonNode tsl,
            boolean strictUnknown,
            String status,
            Instant publishedAt,
            Instant createdAt) {
    }

    public record BindingRow(
            UUID profileId,
            UUID assetId,
            UUID ruleChainId,
            String schemaVersion,
            String syncResult,
            Instant syncedAt) {
    }

    public record DraftResult(ProductRow product, TslRow tsl) {
    }
}
