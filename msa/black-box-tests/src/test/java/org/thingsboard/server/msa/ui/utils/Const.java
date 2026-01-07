/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.utils;

import static org.thingsboard.server.msa.TestProperties.getBaseUrl;

public class Const {

    public static final String URL = getBaseUrl();
    public static final String TENANT_EMAIL = "tenant@thingsboard.org";
    public static final String TENANT_PASSWORD = "tenant";
    public static final String ENTITY_NAME = "Az!@#$%^&*()_-+=~`";
    public static final String ROOT_RULE_CHAIN_NAME = "Root Rule Chain";
    public static final String IMPORT_RULE_CHAIN_NAME = "Rule Chain For Import";
    public static final String IMPORT_DEVICE_PROFILE_NAME = "Device Profile For Import";
    public static final String IMPORT_ASSET_PROFILE_NAME = "Asset Profile For Import";
    public static final String IMPORT_RULE_CHAIN_FILE_NAME = "ruleChainForImport.json";
    public static final String IMPORT_DEVICE_PROFILE_FILE_NAME = "deviceProfileForImport.json";
    public static final String IMPORT_ASSET_PROFILE_FILE_NAME = "assetProfileForImport.json";
    public static final String IMPORT_TXT_FILE_NAME = "forImport.txt";
    public static final String EMPTY_IMPORT_MESSAGE = "No file selected";
    public static final String EMPTY_RULE_CHAIN_MESSAGE = "Rule chain name should be specified!";
    public static final String EMPTY_CUSTOMER_MESSAGE = "Customer title should be specified!";
    public static final String EMPTY_DEVICE_PROFILE_MESSAGE = "Device profile name should be specified!";
    public static final String EMPTY_ASSET_PROFILE_MESSAGE = "Asset profile name should be specified!";
    public static final String EMPTY_DEVICE_MESSAGE = "Device name should be specified!";
    public static final String DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE = "The rule chain referenced by the device profiles cannot be deleted!";
    public static final String SAME_NAME_WARNING_CUSTOMER_MESSAGE = "Customer with such title already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE = "Device profile with such name already exists!";
    public static final String SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE = "Asset profile with such name already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_MESSAGE = "Device with such name already exists!";
    public static final String PHONE_NUMBER_ERROR_MESSAGE = "Phone number is invalid or not possible";
    public static final String NAME_IS_REQUIRED_MESSAGE = "Name is required.";
    public static final String DEVICE_PROFILE_IS_REQUIRED_MESSAGE = "Device profile is required";
    public static final String DEVICE_ACTIVE_STATE = "Active";
    public static final String DEVICE_INACTIVE_STATE = "Inactive";
    public static final String PUBLIC_CUSTOMER_NAME = "Public";
}
