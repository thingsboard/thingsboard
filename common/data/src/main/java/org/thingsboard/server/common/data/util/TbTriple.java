// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbTriple<S, T, C> {

    private S first;
    private T second;
    private C third;

    public static <S, T, C> TbTriple<S, T, C> of(S first, T second, C third) {
        return new TbTriple<>(first, second, third);
    }

}