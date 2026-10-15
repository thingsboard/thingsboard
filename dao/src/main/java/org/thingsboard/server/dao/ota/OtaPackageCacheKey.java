// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ota;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class OtaPackageCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 6733960018642945642L;

    private final OtaPackageId id;

    @Override
    public String toString() {
        return id.toString();
    }

}
