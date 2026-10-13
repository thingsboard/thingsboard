// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Insets {

     public Insets(int margin) {
          this.left = margin;
          this.top = margin;
          this.right = margin;
          this.bottom = margin;
     }

     private int left;
     private int right;
     private int top;
     private int bottom;

}
