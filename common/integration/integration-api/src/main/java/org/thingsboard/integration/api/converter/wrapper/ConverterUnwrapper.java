// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Set;

public interface ConverterUnwrapper {

    TbPair<byte[], UplinkMetaData<Object>> unwrap(byte[] payload, UplinkMetaData metadata) throws Exception;

    Set<String> getKeys();

}
