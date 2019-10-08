package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EdgeId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortEdgeInfo {

    private EdgeId edgeId;
    private String title;
}
