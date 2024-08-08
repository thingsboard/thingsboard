///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Input } from '@angular/core';
import { HasOauth2Clients } from '@shared/models/oauth2.models';

@Component({
  selector: 'tb-client-chips',
  templateUrl: './client-chips.component.html',
  styleUrls: ['./client-chips.component.scss']
})
export class ClientChipsComponent {

  clientsEntityValue?: HasOauth2Clients;

  clientPrefixUrl = '/security-settings/oauth2/clients/details/';

  @Input()
  set clientsEntity(value: HasOauth2Clients) {
    this.clientsEntityValue = value;
  }

  get clientsEntity(): HasOauth2Clients {
    return this.clientsEntityValue;
  }

}
