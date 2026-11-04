// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.dao.trendz.TrendzApiService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzApiService implements TrendzApiService {
    private final TrendzClient trendzClient;

    @Override
    public TrendzViewConfig getViewById(User user, UUID viewId) throws ThingsboardException {
        return trendzClient.getTrendzViewById(viewId, user);
    }

    @Override
    public PageData<TrendzViewConfigLite> getAllViews(User user, PageLink pageLink) throws ThingsboardException {
        return trendzClient.getAllTrendzViews(pageLink, user)
                .toPageData();
    }

    @Override
    public TrendzSummary getTrendzSummary(User user) throws ThingsboardException {
        return trendzClient.getTrendzSummary(user);
    }

    @Override
    public TrendzUsage getTrendzUsage(User user) throws ThingsboardException {
        return trendzClient.getTrendzUsage(user);
    }
}
