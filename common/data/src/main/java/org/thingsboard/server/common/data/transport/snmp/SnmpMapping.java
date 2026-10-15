// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.DataType;

import java.io.Serializable;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnmpMapping implements Serializable {

    private static final long serialVersionUID = 2042438869374145944L;

    private String oid;
    private String key;
    private DataType dataType;

    private static final Pattern OID_PATTERN = Pattern.compile("^\\.?([0-2])((\\.0)|(\\.[1-9][0-9]*))*$");

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotEmpty(oid) && OID_PATTERN.matcher(oid).matches() && StringUtils.isNotBlank(key);
    }
}
