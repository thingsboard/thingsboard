/*
 * Copyright © 2016-2017 Ganesh hegde - HashmapInc Authors
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
import '../dashboard/dashboard.scss';

import uiRouter from 'angular-ui-router';
import thingsboardGrid from '../components/grid.directive';
import thingsboardApiUser from '../api/user.service';
import thingsboardApiDevice from '../api/device.service';
import thingsboardApiCustomer from '../api/customer.service';
import thingsboardApiApplication from '../api/application.service';

import thingsboardApiWidget from '../api/widget.service';
import thingsboardApiDashboard from '../api/dashboard.service';
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
import dashboardLayouts from '../dashboard/layouts';
import dashboardStates from '../dashboard/states';

import ApplicationRoutes from './application.routes';

import {ApplicationController} from './application.controller';
import ApplicationDirective from './application.directive';

import thingsboardPluginSelect from '../components/plugin-select.directive';
import thingsboardComponent from '../component';
import thingsboardApiRule from '../api/rule.service';
import thingsboardApiPlugin from '../api/plugin.service';
import thingsboardApiComponentDescriptor from '../api/component-descriptor.service';

import DashboardController from '../dashboard/dashboard.controller';
import DashboardSettingsController from '../dashboard/dashboard-settings.controller';
import AddWidgetController from '../dashboard/add-widget.controller';
//import DashboardDirective from '../dashboard/dashboard.directive';
//import EditWidgetDirective from '../dashboard/edit-widget.directive';
//import DashboardToolbar from '../dashboard/dashboard-toolbar.directive';

//import RuleRoutes from '../rule/rule.routes';
//import RuleController from '../rule/rule.controller';
//import RuleDirective from '../rule/rule.directive';

export default angular.module('thingsboard.application', [
    uiRouter,
    thingsboardGrid,
    thingsboardApiUser,
    thingsboardApiDevice,
    thingsboardApiApplication,
    thingsboardApiCustomer,
    thingsboardPluginSelect,
    thingsboardComponent,
    thingsboardApiRule,
    thingsboardApiPlugin,
    thingsboardApiComponentDescriptor,
    thingsboardTypes,
    thingsboardItemBuffer,
    thingsboardImportExport,
    thingsboardApiWidget,
    thingsboardApiDashboard,
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
    .config(ApplicationRoutes)
    .controller('ApplicationController', ApplicationController)
    .controller('DashboardController', DashboardController)
    .controller('DashboardSettingsController', DashboardSettingsController)
    .controller('AddWidgetController', AddWidgetController)
    .directive('tbApplication', ApplicationDirective)
  //  .directive('tbRule', RuleDirective)
  //  .directive('tbDashboardDetails', DashboardDirective)
   // .directive('tbEditWidget', EditWidgetDirective)
   // .directive('tbDashboardToolbar', DashboardToolbar)
    .name;
