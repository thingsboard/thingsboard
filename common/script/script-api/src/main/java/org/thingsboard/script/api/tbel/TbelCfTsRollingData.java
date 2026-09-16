// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static org.thingsboard.script.api.tbel.TbelCfTsDoubleVal.OBJ_SIZE;

public class TbelCfTsRollingData implements TbelCfObject, Iterable<TbelCfTsMultiDoubleVal> {

    @Getter
    private final TbTimeWindow timeWindow;
    @Getter
    private final List<TbelCfTsMultiDoubleVal> values;

    public TbelCfTsRollingData(TbTimeWindow timeWindow, List<TbelCfTsMultiDoubleVal> values) {
        this.timeWindow = timeWindow;
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    public long memorySize() {
        return 12 + values.size() * OBJ_SIZE;
    }

    @JsonIgnore
    public List<TbelCfTsMultiDoubleVal> getValue() {
        return values;
    }

    @JsonIgnore
    public int getSize() {
        return values.size();
    }

    @Override
    public Iterator<TbelCfTsMultiDoubleVal> iterator() {
        return values.iterator();
    }

}
