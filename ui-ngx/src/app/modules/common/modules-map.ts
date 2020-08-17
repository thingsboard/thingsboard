///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import * as AngularCore from '@angular/core';
import * as AngularCommon from '@angular/common';
import * as AngularForms from '@angular/forms';
import * as AngularRouter from '@angular/router';
import * as AngularCdkKeycodes from '@angular/cdk/keycodes';
import * as AngularCdkCoercion from '@angular/cdk/coercion';
import * as AngularMaterialChips from '@angular/material/chips';
import * as AngularMaterialAutocomplete from '@angular/material/autocomplete';
import * as AngularMaterialDialog from '@angular/material/dialog';
import * as NgrxStore from '@ngrx/store';
import * as RxJs from 'rxjs';
import * as RxJsOperators from 'rxjs/operators';
import * as TranslateCore from '@ngx-translate/core';
import * as TbCore from '@core/public-api';
import * as TbShared from '@shared/public-api';
import * as TbHomeComponents from '@home/components/public-api';
import * as _moment from 'moment';

declare const SystemJS;

export const modulesMap: {[key: string]: any} = {
  '@angular/core': SystemJS.newModule(AngularCore),
  '@angular/common': SystemJS.newModule(AngularCommon),
  '@angular/forms': SystemJS.newModule(AngularForms),
  '@angular/router': SystemJS.newModule(AngularRouter),
  '@angular/cdk/keycodes': SystemJS.newModule(AngularCdkKeycodes),
  '@angular/cdk/coercion': SystemJS.newModule(AngularCdkCoercion),
  '@angular/material/chips': SystemJS.newModule(AngularMaterialChips),
  '@angular/material/autocomplete': SystemJS.newModule(AngularMaterialAutocomplete),
  '@angular/material/dialog': SystemJS.newModule(AngularMaterialDialog),
  '@ngrx/store': SystemJS.newModule(NgrxStore),
  rxjs: SystemJS.newModule(RxJs),
  'rxjs/operators': SystemJS.newModule(RxJsOperators),
  '@ngx-translate/core': SystemJS.newModule(TranslateCore),
  '@core/public-api': SystemJS.newModule(TbCore),
  '@shared/public-api': SystemJS.newModule(TbShared),
  '@home/components/public-api': SystemJS.newModule(TbHomeComponents),
  moment: SystemJS.newModule(_moment)
};
