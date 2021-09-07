/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClassnameFilters({
        "org.thingsboard.server.dao.service.psql.*SqlTest",
        "org.thingsboard.server.dao.service.attributes.psql.*SqlTest",
        "org.thingsboard.server.dao.service.event.psql.*SqlTest",
        "org.thingsboard.server.dao.service.timeseries.psql.*SqlTest"
})
public class PostgreSqlDaoServiceTestSuite {

    @ClassRule
    public static CustomSqlUnit sqlUnit = new CustomSqlUnit(
            Arrays.asList("sql/schema-ts-psql.sql", "sql/schema-entities.sql", "sql/schema-entities-idx.sql"
                    , "sql/system-data.sql"
                    , "sql/system-test-psql.sql"
            ),
            "sql/psql/drop-all-tables.sql",
            "psql-test.properties"
    );

//    @ClassRule
//    public static CustomSqlUnit sqlUnit = new CustomSqlUnit(
//            Arrays.asList("sql/schema-ts-psql.sql"
//                    , "sql/schema-entities.sql", "sql/schema-entities-idx.sql"
//                    , "sql/system-data.sql", "sql/system-test.sql"
//            ),
//            "sql/psql/drop-all-tables.sql",
//            "sql-test.properties"
//    );

//    @ClassRule
//    public static CustomSqlUnit sqlUnit = new CustomSqlUnit(
//            Arrays.asList("sql/schema-timescale.sql", "sql/schema-entities.sql", "sql/schema-entities-idx.sql", "sql/system-data.sql", "sql/system-test.sql"),
//            "sql/timescale/drop-all-tables.sql",
//            "sql-test.properties"
//    );

}
