// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { AgentApplicationType } from '@shared/models/agent.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';

const DOCKER_TAB_INDEX = 1;

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

  notShowAgain = false;
  tabIndex = 0;
  instructionsMethod = EdgeInstructionsMethod;
  contentData: any = {};

  agentAppType = AgentApplicationType.EDGE;

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
      this.tabIndex = DOCKER_TAB_INDEX;
    } else {
      this.dialogTitle = 'edge.install-connect-instructions';
      this.showDontShowAgain = false;
    }
  }

  ngOnInit() {
    const method = this.methodForTab(this.tabIndex);
    if (method) {
      this.getInstructions(method);
    }
  }

  get relatedEntity(): EntityId {
    return { id: this.data.edge.id.id, entityType: EntityType.EDGE };
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

  private methodForTab(index: number): string | null {
    if (index <= 0) {
      return null;
    }
    return this.instructionsMethod[index - 1];
  }

  selectedTabChange(index: number) {
    const method = this.methodForTab(index);
    if (method) {
      this.getInstructions(method);
    }
  }

  getInstructions(method: string) {
    if (!this.contentData[method]) {
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
      });
    }
  }
}
