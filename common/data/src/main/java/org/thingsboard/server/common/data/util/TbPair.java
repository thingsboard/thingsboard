// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbPair<S, T> {
    private S first;
    private T second;

    public static <S, T> TbPair<S, T> of(S first, T second) {
        return new TbPair<>(first, second);
    }
}
