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
import uiRouter from 'angular-ui-router';
import thingsboardGrid from '../components/grid.directive';
import thingsboardPluginSelect from '../components/plugin-select.directive';
import thingsboardComponent from '../component';
import thingsboardApiRule from '../api/rule.service';
import thingsboardApiPlugin from '../api/plugin.service';
import thingsboardApiComponentDescriptor from '../api/component-descriptor.service';

import RuleRoutes from './rule.routes';
import RuleController from './rule.controller';
import RuleDirective from './rule.directive';

export default angular.module('thingsboard.rule', [
    uiRouter,
    thingsboardGrid,
    thingsboardPluginSelect,
    thingsboardComponent,
    thingsboardApiRule,
    thingsboardApiPlugin,
    thingsboardApiComponentDescriptor
])
    .config(RuleRoutes)
    .controller('RuleController', RuleController)
    .directive('tbRule', RuleDirective)
    .name;
