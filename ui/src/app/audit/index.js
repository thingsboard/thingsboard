/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import AuditLogRoutes from './audit-log.routes';
import AuditLogsController from './audit-logs.controller';
import AuditLogDetailsDialogController from './audit-log-details-dialog.controller';
import AuditLogHeaderDirective from './audit-log-header.directive';
import AuditLogRowDirective from './audit-log-row.directive';
import AuditLogTableDirective from './audit-log-table.directive';

export default angular.module('thingsboard.auditLog', [])
    .config(AuditLogRoutes)
    .controller('AuditLogsController', AuditLogsController)
    .controller('AuditLogDetailsDialogController', AuditLogDetailsDialogController)
    .directive('tbAuditLogHeader', AuditLogHeaderDirective)
    .directive('tbAuditLogRow', AuditLogRowDirective)
    .directive('tbAuditLogTable', AuditLogTableDirective)
    .name;
