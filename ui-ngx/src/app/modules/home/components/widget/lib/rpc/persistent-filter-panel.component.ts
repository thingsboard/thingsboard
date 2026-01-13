///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { OverlayRef } from '@angular/cdk/overlay';
import { RpcStatus, rpcStatusTranslation } from '@shared/models/rpc.models';
import { TranslateService } from '@ngx-translate/core';

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

  public persistentFilterFormGroup: UntypedFormGroup;
  public result: PersistentFilterPanelData;
  public rpcSearchStatusTranslationMap = rpcStatusTranslation;
  public rpcSearchPlaceholder: string;

  public persistentSearchStatuses = Object.keys(RpcStatus);

  constructor(@Inject(PERSISTENT_FILTER_PANEL_DATA)
              public data: PersistentFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    this.persistentFilterFormGroup = this.fb.group(
      {
        rpcStatus: this.data.rpcStatus
      }
    );
    this.rpcSearchPlaceholder = this.translate.instant('widgets.persistent-table.any-status');
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

