package org.thingsboard.server.controller.cusomize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StateSystemDTO {
    // tong so vuon
    private int totalFarm;
    // tong so thiet bi
    private int totalDevice;
    // tong so thiet bi hoat dong
    private int totalDeviceActive;
    // tong so canh bao trong tuan qua
    private int totalWarning;
}
