// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.UUID;

/**
 * Re-points solution templates installed from IoT Hub on a 4.2.x platform to their 4.3.x Hub items.
 * <p>
 * IoT Hub publishes solution templates per platform version family: a template for 4.2 is one Hub item
 * (maxTbVersion 430) and the same template for 4.3 is a different item (minTbVersion 430) with its own id.
 * The UI asks the Hub only for items matching the running platform version and marks a card "Installed"
 * when a row in {@code iot_hub_installed_item} carries the same {@code item_id}. After a 4.2 -> 4.3 upgrade the
 * rows still hold the 4.2 item ids, the Hub no longer lists those items, so every installed template loses its
 * "Installed" badge and can be installed a second time.
 * <p>
 * The mapping below pairs each 4.2 item with its 4.3 successor (same Hub listing, same CE/PE edition) and was
 * resolved once against https://iot-hub.thingsboard.io so the upgrade needs no network access. Items published
 * for both families under one id (e.g. "Cold chain solution", "Silo monitoring") need no remap and are absent.
 * <p>
 * Only {@code item_id} (and the display name) moves. {@code item_version_id} keeps pointing at the 4.2 package that
 * is really installed, so the card shows "Installed" together with "Update available" and the tenant decides
 * whether to reinstall the 4.3 package; the version endpoint is not filtered by platform version, so the old id
 * still resolves.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_5Migration implements LtsMigration {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getVersion() {
        return "4.3.1.5";
    }

    @Override
    public void apply() {
        remapSolutionTemplatesToCurrentFamily();
    }

    /**
     * One solution template whose Hub item changed between the 4.2 and 4.3 families.
     *
     * @param listingSlug Hub listing shared by both items, kept for readability and logs
     * @param fromItemId  item id the template had for 4.2 (what pre-upgrade rows hold)
     * @param toItemId    item id of the 4.3 successor (what the Hub lists on 4.3)
     * @param toItemName  successor title, as shown in the "Installed items" table
     */
    record SolutionTemplateMove(String listingSlug,
                                UUID fromItemId,
                                UUID toItemId,
                                String toItemName) {}

    static final List<SolutionTemplateMove> SOLUTION_TEMPLATE_MOVES = List.of(
            // --- Community Edition items ---
            move("temperature-sensors", "0dedeb3c-5373-4bfd-86a7-e8b65273a828", "aeae4d0f-99f4-405a-9f34-670b2c8a7121", "Temperature & Humidity sensors"),
            move("smart-office", "3b4caf6f-4449-4bbd-8c99-4dd9a3f82a27", "43e6f5ff-20a0-41c1-94e1-e7c9b3751cdd", "Smart office"),
            move("fleet-tracking", "c0ac26b1-768b-4bd3-bb91-f5cf42854d68", "aef261f4-c6c4-4683-9618-8e18a6d1333a", "Fleet tracking"),
            move("fuel-level-monitoring", "85a504a5-2316-4244-a2b0-8af929f6f04c", "71e8e081-d0d1-4a63-8868-3b0853517d9b", "Fuel level monitoring"),
            move("swimming-pool-scada-system", "81e67a4e-52a2-45bb-a603-1d8e421db801", "06518e23-47b0-41e3-ad02-1494217a99a5", "Swimming pool SCADA system"),
            // --- Professional Edition items (never present on a CE database; kept so the PE merge inherits them) ---
            move("temperature-sensors", "7395c99f-235f-499e-b1e5-c9117a0b7e07", "9a64ef1f-c926-45fb-9a5f-0e88e40c485d", "Temperature & Humidity sensors"),
            move("smart-office", "bd2c54df-4b34-4cb6-9f2f-596f0bad4cd4", "bfb5793e-5b99-4d11-8ce3-079bfcd49257", "Smart office"),
            move("fleet-tracking", "51e81996-d9b7-4972-9716-bf2854819c7e", "f7589b43-0bbe-45fe-90bb-2bbbb563f361", "Site fleet tracking"),
            move("fuel-level-monitoring", "5765a34c-490e-4c98-8ba5-17036deaf5ce", "d41ad82d-d86a-44df-bc01-d2262de48162", "Fuel level monitoring"),
            move("swimming-pool-scada-system", "f476dc19-e38c-46af-9818-316d4c57c988", "5f958baa-ee8a-417e-961f-9d553bbac7e4", "Swimming pool SCADA system"),
            move("scada-drilling-system", "0f6cad80-cb62-4eb6-ba9d-2372e3bc329e", "3b24a28e-6ad8-42c0-90f7-88d63901d4e9", "SCADA Oil & Gas drilling system"),
            move("scada-energy-management", "a0344f65-1947-4a75-afe3-a74c8ae39a8f", "b9a79174-68d6-43d4-b0bb-288958a70787", "SCADA Energy management"),
            move("air-quality-index", "ff97f8cd-71b3-49fe-9bcf-f0e87340efbe", "70066f96-adc8-4d29-8975-4753b326e8c8", "Air quality monitoring"),
            move("water-metering", "77b3cff4-9e78-44b5-872e-57b7f400f938", "9acfcdf8-660b-47d5-8f23-1ef92c5c4f38", "Water metering"),
            move("smart-retail", "029f443a-8e26-41c1-8382-a5f500f8c104", "d54a8c77-9565-4142-8dd2-8be69c2b6211", "Smart retail"),
            move("smart-irrigation", "364ab326-699c-4320-b7f6-6e64e90fa21a", "e3d61121-55e4-42dc-81f1-86f088909c4d", "Smart irrigation"),
            move("assisted-living", "51750ead-5f6d-44a6-bb67-78d7d8bb8dc9", "4aed1971-57a6-4030-8d47-a854356a2a22", "Assisted living"),
            move("waste-monitoring", "a610d1ad-8fe7-45cf-b839-c6af323b959b", "993fee8b-a0de-43c8-a113-72db46d7ceb3", "Waste management"),
            move("generator-monitoring", "43fa7c70-7c60-11f1-ad69-cb6d3580c9b4", "1aafceb0-7c60-11f1-bf7c-01757b5ced76", "Generator monitoring")
    );

    // The NOT EXISTS guard keeps a tenant that already installed the 4.3 item (possible on 4.3.1.3 / 4.3.1.4,
    // where the 4.2 row silently stayed behind) from ending up with two rows for the same item.
    // The descriptor is left untouched: it lists the entities the install created and is what uninstall needs.
    static final String REMAP_SQL = """
            UPDATE iot_hub_installed_item i
            SET item_id = ?, item_name = ?
            WHERE i.item_type = 'SOLUTION_TEMPLATE' AND i.item_id = ?
              AND NOT EXISTS (SELECT 1 FROM iot_hub_installed_item n
                              WHERE n.tenant_id = i.tenant_id AND n.item_id = ?)""";

    private void remapSolutionTemplatesToCurrentFamily() {
        int total = 0;
        for (SolutionTemplateMove move : SOLUTION_TEMPLATE_MOVES) {
            int updated = jdbcTemplate.update(REMAP_SQL,
                    move.toItemId(), move.toItemName(), move.fromItemId(), move.toItemId());
            if (updated > 0) {
                log.info("Re-pointed {} installed solution template(s) '{}' from Hub item {} to {}",
                        updated, move.listingSlug(), move.fromItemId(), move.toItemId());
                total += updated;
            }
        }
        log.info("IoT Hub installed solution templates re-pointed to 4.3 items: {}", total);
    }

    private static SolutionTemplateMove move(String listingSlug, String fromItemId, String toItemId, String toItemName) {
        return new SolutionTemplateMove(listingSlug, UUID.fromString(fromItemId), UUID.fromString(toItemId), toItemName);
    }

}
