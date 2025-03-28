package org.thingsboard.server.controller.cusomize;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api/customize")
@RequiredArgsConstructor
@Slf4j
public class CustomizeController extends BaseController {

    // get current state of assets detail
    @ApiOperation(value = "Get current state of assets detail")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/state-system")
    public StateSystemDTO getStateChain() {
        return new StateSystemDTO(10, 100, 50, 5);
    }


}
