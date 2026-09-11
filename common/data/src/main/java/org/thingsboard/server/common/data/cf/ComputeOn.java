/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.cf;

/**
 * Defines which side of an edge deployment computes a calculated field.
 * <p>
 * {@link #DEFAULT} and {@link #isComputedHere(ComputeOn)} are deliberately inverted in the
 * thingsboard-edge fork (EDGE instead of CLOUD). Keep them as the single seam between the two
 * builds - inlining {@code isComputedHere} into its call sites, or rewriting it as
 * {@code computeOn != EDGE}, would silently break the edge with nothing here to catch it.
 */
public enum ComputeOn {

    CLOUD,
    EDGE;

    public static final ComputeOn DEFAULT = CLOUD;

    public static ComputeOn orDefault(ComputeOn computeOn) {
        return computeOn == null ? DEFAULT : computeOn;
    }

    public static boolean isComputedHere(ComputeOn computeOn) {
        return orDefault(computeOn) == CLOUD;
    }

}
