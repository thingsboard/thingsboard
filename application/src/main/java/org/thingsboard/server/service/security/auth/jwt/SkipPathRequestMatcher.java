// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class SkipPathRequestMatcher implements RequestMatcher {

    private final OrRequestMatcher skipMatchers;
    private final OrRequestMatcher processMatchers;

    public SkipPathRequestMatcher(List<String> pathsToSkip, List<String> pathsToProcess) {
        Assert.notNull(pathsToSkip, "List of paths to skip is required.");
        List<RequestMatcher> skip = pathsToSkip.stream().map(AntPathRequestMatcher::new).collect(Collectors.toList());
        List<RequestMatcher> process = pathsToProcess.stream().map(AntPathRequestMatcher::new).collect(Collectors.toList());
        skipMatchers = new OrRequestMatcher(skip);
        processMatchers = new OrRequestMatcher(process);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (skipMatchers.matches(request)) {
            return false;
        }
        return processMatchers.matches(request);
    }

}
