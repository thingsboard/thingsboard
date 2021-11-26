///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, InjectionToken } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { OverlayRef } from '@angular/cdk/overlay';
import { RpcStatus, rpcStatusTranslation } from '@shared/models/rpc.models';

export const PERSISTENT_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface PersistentFilterPanelData {
  rpcStatus: RpcStatus;
}

@Component({
  selector: 'tb-persistent-filter-panel',
  templateUrl: './persistent-filter-panel.component.html',
  styleUrls: ['./persistent-filter-panel.component.scss']
})
export class PersistentFilterPanelComponent {

  public persistentFilterFormGroup: FormGroup;
  public result: PersistentFilterPanelData;
  public rpcSearchStatusTranslationMap = rpcStatusTranslation;

  public persistentSearchStatuses = [
    RpcStatus.QUEUED,
    RpcStatus.SENT,
    RpcStatus.DELIVERED,
    RpcStatus.SUCCESSFUL,
    RpcStatus.TIMEOUT,
    RpcStatus.EXPIRED,
    RpcStatus.FAILED
  ];

  constructor(@Inject(PERSISTENT_FILTER_PANEL_DATA)
              public data: PersistentFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: FormBuilder) {
    this.persistentFilterFormGroup = this.fb.group(
      {
        rpcStatus: this.data.rpcStatus
      }
    );
  }

  update() {
    this.result = {
      rpcStatus: this.persistentFilterFormGroup.get('rpcStatus').value
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}

