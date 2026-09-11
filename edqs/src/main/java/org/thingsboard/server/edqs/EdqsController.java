// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.edqs.state.EdqsStateService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edqs")
public class EdqsController {

    private final EdqsStateService edqsStateService;

    @GetMapping("/ready")
    public ResponseEntity<Void> isReady() {
        if (edqsStateService.isReady()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

}
