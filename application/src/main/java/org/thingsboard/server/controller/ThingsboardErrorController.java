/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardErrorResponse;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class ThingsboardErrorController implements ErrorController {

    public static final String PATH_NOT_FOUND_ERROR_DESCRIPTION = "Path is not found.";
    public static final String GENERAL_ERROR_DESCRIPTION = "Something went wrong! Our Engineers are on it";

    @RequestMapping("/error")
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                return new ResponseEntity<>(ThingsboardErrorResponse.of(PATH_NOT_FOUND_ERROR_DESCRIPTION, ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND), httpHeaders, HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(ThingsboardErrorResponse.of(GENERAL_ERROR_DESCRIPTION, ThingsboardErrorCode.GENERAL, HttpStatus.INTERNAL_SERVER_ERROR), httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
