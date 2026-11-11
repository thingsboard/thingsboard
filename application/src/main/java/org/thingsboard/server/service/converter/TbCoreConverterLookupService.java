// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Service
@RequiredArgsConstructor
public class TbCoreConverterLookupService implements ConverterLookupService {

    private final ConverterService converterService;

    @Override
    public Converter findConverterById(TenantId tenantId, ConverterId converterId) {
        return converterService.findConverterById(tenantId, converterId);
    }

}
