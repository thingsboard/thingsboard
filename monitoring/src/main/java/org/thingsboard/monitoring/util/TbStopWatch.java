// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.util;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TbStopWatch {

    private final StopWatch internal = new StopWatch();

    public void start() {
        internal.reset();
        internal.start();
    }

    public long getTime() {
        internal.stop();
        long nanoTime = internal.getNanoTime();
        internal.reset();
        return nanoTime;
    }

}
