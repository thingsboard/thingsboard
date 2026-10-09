// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface TBUplinkDataConverter extends TBDataConverter {

    ListenableFuture<List<UplinkData>> convertUplink(ConverterContext context, byte[] data, UplinkMetaData metadata,
                                                     ExecutorService callBackExecutorService) throws Exception;

}
