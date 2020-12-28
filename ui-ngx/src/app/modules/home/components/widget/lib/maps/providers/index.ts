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

import { OpenStreetMap } from './openstreet-map';
import { TencentMap } from './tencent-map';
import { GoogleMap } from './google-map';
import { HEREMap } from './here-map';
import { ImageMap } from './image-map';
import { Type } from '@angular/core';
import LeafletMap from '@home/components/widget/lib/maps/leaflet-map';

export const providerClass: { [key: string]: Type<LeafletMap> } = {
  'openstreet-map': OpenStreetMap,
  'tencent-map': TencentMap,
  'google-map': GoogleMap,
  here: HEREMap,
  'image-map': ImageMap
};
