/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import './widget-editor.scss';

import 'angular-hotkeys';
import 'angular-ui-ace';

import uiRouter from 'angular-ui-router';
import thingsboardApiUser from '../api/user.service';
import thingsboardApiWidget from '../api/widget.service';
import thingsboardTypes from '../common/types.constant';
import thingsboardToast from '../services/toast';
import thingsboardConfirmOnExit from '../components/confirm-on-exit.directive';
import thingsboardDashboard from '../components/dashboard.directive';
import thingsboardExpandFullscreen from '../components/expand-fullscreen.directive';
import thingsboardCircularProgress from '../components/circular-progress.directive';
import thingsboardMdChipDraggable from '../components/md-chip-draggable.directive';

import WidgetLibraryRoutes from './widget-library.routes';
import WidgetLibraryController from './widget-library.controller';
import SelectWidgetTypeController from './select-widget-type.controller';
import WidgetEditorController from './widget-editor.controller';
import WidgetsBundleController from './widgets-bundle.controller';
import WidgetsBundleDirective from './widgets-bundle.directive';
import SaveWidgetTypeAsController from './save-widget-type-as.controller';

export default angular.module('thingsboard.widget-library', [
    uiRouter,
    thingsboardApiWidget,
    thingsboardApiUser,
    thingsboardTypes,
    thingsboardToast,
    thingsboardConfirmOnExit,
    thingsboardDashboard,
    thingsboardExpandFullscreen,
    thingsboardCircularProgress,
    thingsboardMdChipDraggable,
    'cfp.hotkeys',
    'ui.ace'
])
    .config(WidgetLibraryRoutes)
    .controller('WidgetLibraryController', WidgetLibraryController)
    .controller('SelectWidgetTypeController', SelectWidgetTypeController)
    .controller('WidgetEditorController', WidgetEditorController)
    .controller('WidgetsBundleController', WidgetsBundleController)
    .controller('SaveWidgetTypeAsController', SaveWidgetTypeAsController)
    .directive('tbWidgetsBundle', WidgetsBundleDirective)
    .name;
