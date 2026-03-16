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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import {
  EdgeInfo,
  EdgeInstructions,
  EdgeInstructionsMethod,
  edgeVersionAttributeKey
} from '@shared/models/edge.models';
import { EdgeService } from '@core/http/edge.service';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { mergeMap, Observable } from 'rxjs';

export interface EdgeInstructionsDialogData {
  edge: EdgeInfo;
  afterAdd: boolean;
  upgradeAvailable: boolean;
}

@Component({
    selector: 'tb-edge-installation-dialog',
    templateUrl: './edge-instructions-dialog.component.html',
    styleUrls: ['./edge-instructions-dialog.component.scss'],
    standalone: false
})
export class EdgeInstructionsDialogComponent extends DialogComponent<EdgeInstructionsDialogComponent> implements OnInit, OnDestroy {

  dialogTitle: string;
  showDontShowAgain: boolean;

  loadedInstructions = false;
  notShowAgain = false;
  tabIndex = 0;
  instructionsMethod = EdgeInstructionsMethod;
  contentData: any = {};

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: EdgeInstructionsDialogData,
              public dialogRef: MatDialogRef<EdgeInstructionsDialogComponent>,
              private attributeService: AttributeService,
              private edgeService: EdgeService) {
    super(store, router, dialogRef);

    if (this.data.afterAdd) {
      this.dialogTitle = 'edge.install-connect-instructions-edge-created';
      this.showDontShowAgain = true;
    } else if (this.data.upgradeAvailable) {
      this.dialogTitle = 'edge.upgrade-instructions';
      this.showDontShowAgain = false;
    } else {
      this.dialogTitle = 'edge.install-connect-instructions';
      this.showDontShowAgain = false;
    }
  }

  ngOnInit() {
    this.getInstructions(this.instructionsMethod[this.tabIndex]);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({notDisplayInstructionsAfterAddEdge: true}));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  selectedTabChange(index: number) {
    this.getInstructions(this.instructionsMethod[index]);
  }

  getInstructions(method: string) {
    if (!this.contentData[method]) {
      this.loadedInstructions = false;
      let edgeInstructions$: Observable<EdgeInstructions>;
      if (this.data.upgradeAvailable) {
        edgeInstructions$ = this.attributeService.getEntityAttributes(this.data.edge.id, AttributeScope.SERVER_SCOPE, [edgeVersionAttributeKey])
          .pipe(mergeMap(attributes => {
            if (attributes.length) {
              const edgeVersion = attributes[0].value;
              return this.edgeService.getEdgeUpgradeInstructions(edgeVersion, method);
            }
          }));
      } else {
        edgeInstructions$ = this.edgeService.getEdgeInstallInstructions(this.data.edge.id.id, method);
      }
      edgeInstructions$.subscribe(res => {
        this.contentData[method] = res.instructions;
        this.loadedInstructions = true;
      });
    }
  }
}
