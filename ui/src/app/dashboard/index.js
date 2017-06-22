/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import './dashboard.scss';

import uiRouter from 'angular-ui-router';

import thingsboardGrid from '../components/grid.directive';
import thingsboardApiWidget from '../api/widget.service';
import thingsboardApiUser from '../api/user.service';
import thingsboardApiDashboard from '../api/dashboard.service';
import thingsboardApiCustomer from '../api/customer.service';
import thingsboardDetailsSidenav from '../components/details-sidenav.directive';
import thingsboardWidgetConfig from '../components/widget/widget-config.directive';
import thingsboardDashboardSelect from '../components/dashboard-select.directive';
import thingsboardRelatedEntityAutocomplete from '../components/related-entity-autocomplete.directive';
import thingsboardDashboard from '../components/dashboard.directive';
import thingsboardExpandFullscreen from '../components/expand-fullscreen.directive';
import thingsboardWidgetsBundleSelect from '../components/widgets-bundle-select.directive';
import thingsboardSocialsharePanel from '../components/socialshare-panel.directive';
import thingsboardTypes from '../common/types.constant';
import thingsboardItemBuffer from '../services/item-buffer.service';
import thingsboardImportExport from '../import-export';
import dashboardLayouts from './layouts';
import dashboardStates from './states';

import DashboardRoutes from './dashboard.routes';
import {DashboardsController, DashboardCardController, MakeDashboardPublicDialogController} from './dashboards.controller';
import DashboardController from './dashboard.controller';
import DashboardSettingsController from './dashboard-settings.controller';
import AssignDashboardToCustomerController from './assign-to-customer.controller';
import AddDashboardsToCustomerController from './add-dashboards-to-customer.controller';
import AddWidgetController from './add-widget.controller';
import DashboardDirective from './dashboard.directive';
import EditWidgetDirective from './edit-widget.directive';
import DashboardToolbar from './dashboard-toolbar.directive';

export default angular.module('thingsboard.dashboard', [
    uiRouter,
    thingsboardTypes,
    thingsboardItemBuffer,
    thingsboardImportExport,
    thingsboardGrid,
    thingsboardApiWidget,
    thingsboardApiUser,
    thingsboardApiDashboard,
    thingsboardApiCustomer,
    thingsboardDetailsSidenav,
    thingsboardWidgetConfig,
    thingsboardDashboardSelect,
    thingsboardRelatedEntityAutocomplete,
    thingsboardDashboard,
    thingsboardExpandFullscreen,
    thingsboardWidgetsBundleSelect,
    thingsboardSocialsharePanel,
    dashboardLayouts,
    dashboardStates
])
    .config(DashboardRoutes)
    .controller('DashboardsController', DashboardsController)
    .controller('DashboardCardController', DashboardCardController)
    .controller('MakeDashboardPublicDialogController', MakeDashboardPublicDialogController)
    .controller('DashboardController', DashboardController)
    .controller('DashboardSettingsController', DashboardSettingsController)
    .controller('AssignDashboardToCustomerController', AssignDashboardToCustomerController)
    .controller('AddDashboardsToCustomerController', AddDashboardsToCustomerController)
    .controller('AddWidgetController', AddWidgetController)
    .directive('tbDashboardDetails', DashboardDirective)
    .directive('tbEditWidget', EditWidgetDirective)
    .directive('tbDashboardToolbar', DashboardToolbar)
    .name;
