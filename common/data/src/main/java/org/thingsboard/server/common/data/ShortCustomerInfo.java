// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Created by igor on 2/27/18.
 */

@Schema
@AllArgsConstructor
public class ShortCustomerInfo {

    @Schema(description = "JSON object with the customer Id.")
    @Getter @Setter
    private CustomerId customerId;

    @Schema(description = "Title of the customer.")
    @Getter @Setter
    @NoXss
    private String title;

    @Schema(description = "Indicates special 'Public' customer used to embed dashboards on public websites.")
    @Getter @Setter
    private boolean isPublic;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShortCustomerInfo that = (ShortCustomerInfo) o;

        return customerId.equals(that.customerId);

    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }
}
