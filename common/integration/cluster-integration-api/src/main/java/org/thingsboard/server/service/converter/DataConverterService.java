// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import org.thingsboard.integration.api.converter.TBDataConverter;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

/**
 * Created by ashvayka on 02.12.17.
 */
public interface DataConverterService {

    TBDataConverter createConverter(Converter converter);

    TBDataConverter updateConverter(Converter converter);

    void deleteConverter(ConverterId converterId);

    Optional<TBUplinkDataConverter> getUplinkConverterById(TenantId tenantId, ConverterId converterId);

    Optional<TBDownlinkDataConverter> getDownlinkConverterById(TenantId tenantId, ConverterId converterId);

}
