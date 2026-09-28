// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.List;

public enum FontWeight {

     NORMAL("normal"), BOLD("bold"), WEIGHT_500("500");

     @Getter
     private final String value;

     FontWeight(String label) {
          this.value = label;
     }

     @JsonValue
     public String getValue() {
          return value;
     }

     @JsonCreator
     public static FontWeight fromLabel(String value) {
          for (FontWeight type : values()) {
               if (type.value.equalsIgnoreCase(value)) {
                    return type;
               }
          }
          if (List.of("lighter", "100", "200", "300", "400").contains(value)) {
               return NORMAL;
          }
          if (List.of("bolder", "600", "700", "800", "900").contains(value)) {
               return BOLD;
          }
          throw new IllegalArgumentException("Unknown FontWeight: " + value);
     }
}
