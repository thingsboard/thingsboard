// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.List;
import java.util.Map;

public interface ConverterLibraryService {

    List<Vendor> getVendors(IntegrationType integrationType, String converterType, int page, int pageSize, boolean loadImages);

    List<Model> getVendorModels(IntegrationType integrationType, String converterType, String vendorName, int page, int pageSize, boolean loadImages);

    String getConverter(IntegrationType integrationType, String converterType, String vendorName, String model);

    String getConverterMetadata(IntegrationType integrationType, String converterType, String vendorName, String model);

    String getPayload(IntegrationType integrationType, String converterType, String vendorName, String model);

    Map<String, LibraryConvertersInfo> getConvertersInfo();

}
