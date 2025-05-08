/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Validator {

    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$"); // Unicode letters, numbers, '_' and '-' allowed

    /**
     * This method validate <code>EntityId</code> entity id. If entity id is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param entityId      the entityId
     * @param errorMessage  the error message for exception
     */
    @Deprecated
    public static void validateEntityId(EntityId entityId, String errorMessage) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>EntityId</code> entity id. If entity id is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param entityId              the entityId
     * @param errorMessageFunction  the error message function for exception that applies entityId
     */
    public static void validateEntityId(EntityId entityId, Function<EntityId, String> errorMessageFunction) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(entityId));
        }
    }

    /**
     * This method validate <code>String</code> string. If string is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val           the val
     * @param errorMessage  the error message for exception
     */
    public static void validateString(String val, String errorMessage) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>String</code> string. If string is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val                   the value
     * @param errorMessageFunction  the error message function that applies value
     */
    public static void validateString(String val, Function<String, String> errorMessageFunction) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessageFunction.apply(val));
        }
    }

    /**
     * This method validate <code>long</code> value. If value isn't positive than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val           the val
     * @param errorMessage  the error message for exception
     */
    public static void validatePositiveNumber(long val, String errorMessage) {
        if (val <= 0) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>UUID</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id            the id
     * @param errorMessage  the error message for exception
     */
    @Deprecated
    public static void validateId(UUID id, String errorMessage) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>UUID</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id                    the id
     * @param errorMessageFunction  the error message function for exception that applies id
     */
    public static void validateId(UUID id, Function<UUID, String> errorMessageFunction) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(id));
        }
    }

    /**
     * This method validate <code>UUIDBased</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id            the id
     * @param errorMessage  the error message for exception
     */
    @Deprecated
    public static void validateId(UUIDBased id, String errorMessage) {
        if (id == null || id.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>UUIDBased</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id                    the id
     * @param errorMessageFunction  the error message function for exception that applies id
     */
    public static void validateId(UUIDBased id, Function<UUIDBased, String> errorMessageFunction) {
        if (id == null || id.getId() == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(id));
        }
    }

    /**
     * This method validate <code>UUIDBased</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id                    the id
     * @param ids                   the list of ids
     * @param errorMessageFunction  the error message function for exception that applies ids
     */
    static void validateId(UUIDBased id, List<? extends UUIDBased> ids, Function<List<? extends UUIDBased>, String> errorMessageFunction) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(ids));
        }
    }

    /**
     * This method validate list of <code>UUIDBased</code> ids. If at least one of the ids is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param ids           the list of ids
     * @param errorMessage  the error message for exception
     */
    @Deprecated
    public static void validateIds(List<? extends UUIDBased> ids, String errorMessage) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        } else {
            for (UUIDBased id : ids) {
                validateId(id, errorMessage);
            }
        }
    }

    /**
     * This method validate list of <code>UUIDBased</code> ids. If at least one of the ids is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param ids                   the list of ids
     * @param errorMessageFunction  the error message function for exception that applies ids
     */
    public static void validateIds(List<? extends UUIDBased> ids, Function<List<? extends UUIDBased>, String> errorMessageFunction) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessageFunction.apply(ids));
        } else {
            for (UUIDBased id : ids) {
                validateId(id, ids, errorMessageFunction);
            }
        }
    }

    /**
     * This method validate <code>PageLink</code> page link. If pageLink is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param pageLink           the page link
     */
    public static void validatePageLink(PageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect page link page size '"+pageLink.getPageSize()+"'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect page link page '"+pageLink.getPage()+"'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null) {
            if (!isValidProperty(pageLink.getSortOrder().getProperty())) {
                throw new IncorrectParameterException("Invalid page link sort property");
            }
        }
    }

    public static void validateEntityDataPageLink(EntityDataPageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Entity Data Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect entity data page link page size '" + pageLink.getPageSize() + "'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect entity data page link page '" + pageLink.getPage() + "'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null && pageLink.getSortOrder().getKey() != null) {
            EntityKey sortKey = pageLink.getSortOrder().getKey();
            if ((sortKey.getType() == EntityKeyType.ENTITY_FIELD || sortKey.getType() == EntityKeyType.ALARM_FIELD)
                    && !isValidProperty(sortKey.getKey())) {
                throw new IncorrectParameterException("Invalid entity data page link sort property");
            }
        }
    }

    public static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

    public static void checkNotNull(Object reference, String errorMessage) {
        if (reference == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

}
