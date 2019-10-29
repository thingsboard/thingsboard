package org.thingsboard.server.service.edge;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.edge.EdgeService;

@Component
@Data
public class EdgeContextComponent {

    @Autowired
    private EdgeService edgeService;
}
