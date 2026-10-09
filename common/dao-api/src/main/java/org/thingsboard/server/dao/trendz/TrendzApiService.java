// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.trendz;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;

import java.util.UUID;

public interface TrendzApiService {
    TrendzViewConfig getViewById(User user, UUID viewId) throws ThingsboardException;

    PageData<TrendzViewConfigLite> getAllViews(User user, PageLink pageLink) throws ThingsboardException;

    TrendzSummary getTrendzSummary(User user) throws ThingsboardException;

    TrendzUsage getTrendzUsage(User user) throws ThingsboardException;
}
