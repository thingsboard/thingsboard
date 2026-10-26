package com.iot.thingmodel.admin;

import com.iot.thingmodel.db.JooqTime;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.iot.thingmodel.db.jooq.Tables.PLAT_VENDOR_ACCOUNT;
import static org.jooq.impl.DSL.excluded;

@Repository
public class TenantAccountRepository {

    private final DSLContext dsl;

    public TenantAccountRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<TenantAccountRow> find(UUID tenantId) {
        return dsl.select(
                        PLAT_VENDOR_ACCOUNT.TENANT_ID,
                        PLAT_VENDOR_ACCOUNT.USERNAME,
                        PLAT_VENDOR_ACCOUNT.PASSWORD_CIPHER,
                        PLAT_VENDOR_ACCOUNT.ENABLED,
                        PLAT_VENDOR_ACCOUNT.CREATED_AT,
                        PLAT_VENDOR_ACCOUNT.UPDATED_AT)
                .from(PLAT_VENDOR_ACCOUNT)
                .where(PLAT_VENDOR_ACCOUNT.TENANT_ID.eq(tenantId))
                .fetchOptional(record -> new TenantAccountRow(
                        record.get(PLAT_VENDOR_ACCOUNT.TENANT_ID),
                        record.get(PLAT_VENDOR_ACCOUNT.USERNAME),
                        record.get(PLAT_VENDOR_ACCOUNT.PASSWORD_CIPHER),
                        Boolean.TRUE.equals(record.get(PLAT_VENDOR_ACCOUNT.ENABLED)),
                        JooqTime.toInstant(record.get(PLAT_VENDOR_ACCOUNT.CREATED_AT)),
                        JooqTime.toInstant(record.get(PLAT_VENDOR_ACCOUNT.UPDATED_AT))));
    }

    public void upsert(UUID tenantId, String username, String passwordCipher) {
        dsl.insertInto(PLAT_VENDOR_ACCOUNT)
                .set(PLAT_VENDOR_ACCOUNT.TENANT_ID, tenantId)
                .set(PLAT_VENDOR_ACCOUNT.USERNAME, username)
                .set(PLAT_VENDOR_ACCOUNT.PASSWORD_CIPHER, passwordCipher)
                .set(PLAT_VENDOR_ACCOUNT.ENABLED, true)
                .set(PLAT_VENDOR_ACCOUNT.UPDATED_AT, JooqTime.now())
                .onConflict(PLAT_VENDOR_ACCOUNT.TENANT_ID)
                .doUpdate()
                .set(PLAT_VENDOR_ACCOUNT.USERNAME, excluded(PLAT_VENDOR_ACCOUNT.USERNAME))
                .set(PLAT_VENDOR_ACCOUNT.PASSWORD_CIPHER, excluded(PLAT_VENDOR_ACCOUNT.PASSWORD_CIPHER))
                .set(PLAT_VENDOR_ACCOUNT.ENABLED, true)
                .set(PLAT_VENDOR_ACCOUNT.UPDATED_AT, JooqTime.now())
                .execute();
    }

    public record TenantAccountRow(
            UUID tenantId,
            String username,
            String passwordCipher,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
    }
}
