// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  EventEmitter,
  Inject,
  Input,
  Optional,
  Output
} from '@angular/core';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { StepperOrientation } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MediaBreakpoints } from '@shared/models/constants';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEvent,
  AgentApplicationType,
  AgentInfo
} from '@shared/models/agent.models';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import {
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult,
  AgentAppUpgradeResult,
  AgentAppWizardFinish
} from '@home/pages/agent/wizard/agent-app-wizard.models';

export type {
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult,
  AgentAppUpgradeResult
} from '@home/pages/agent/wizard/agent-app-wizard.models';
export { isAgentAppUpgradeResult } from '@home/pages/agent/wizard/agent-app-wizard.models';

// Thin dispatcher: keeps the public contract (selector, inputs, finished /
// cancelled outputs, dialog plumbing) and renders the per-mode flow component.
// All mode-specific steps and logic live in the flow components; this owns the
// dialog/embedded completion (close + optional navigate / progress dialog).
@Component({
  selector: 'tb-agent-app-install-wizard',
  templateUrl: './agent-app-install-wizard.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss'],
  standalone: false,
  providers: [AgentAppWizardLoaderService, AgentAppWizardSubmitService]
})
export class AgentAppInstallWizardComponent {

  @Input() agentId: string;
  @Input() agent: AgentInfo;
  @Input() mode: 'install' | 'update' | 'upgrade' = 'install';
  @Input() application: AgentApplication | null = null;
  @Input() lockedType: AgentApplicationType | null = null;
  @Input() lockedRelatedEntity: EntityId | null = null;
  @Input() navigateToAgentOnFinish = false;
  @Input() selectAgent = false;
  @Input() embedded = false;
  @Input() showBack = false;

  @Output() finished = new EventEmitter<AgentAppInstallWizardResult>();
  @Output() cancelled = new EventEmitter<void>();

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;

  constructor(private router: Router,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver,
              @Optional() @Inject(MAT_DIALOG_DATA) public data: AgentAppInstallWizardData | null,
              @Optional() public dialogRef: MatDialogRef<AgentAppInstallWizardComponent, AgentAppInstallWizardResult> | null) {
    if (this.data) {
      this.agentId = this.data.agentId;
      this.agent = this.data.agent;
      this.mode = this.data.mode || 'install';
      this.application = this.data.application || null;
      this.lockedType = this.data.lockedType || null;
      this.lockedRelatedEntity = this.data.lockedRelatedEntity || null;
      this.navigateToAgentOnFinish = !!this.data.navigateToAgentOnFinish;
      this.selectAgent = !!this.data.selectAgent;
      this.showBack = !!this.data.showBack;
    }
    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'horizontal' : 'vertical'));
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
  }

  onFinished(f: AgentAppWizardFinish) {
    this.closeOrEmit(f.event);
    if (f.closeOnly) {
      return;
    }
    if (this.navigateToAgentOnFinish) {
      const agentId = f.application?.agentId?.id || this.agentId;
      if (agentId) {
        this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId));
      }
      return;
    }
    if (f.application && f.event) {
      openAgentAppEventProgress(this.dialog, f.application, f.event).subscribe();
    }
  }

  onCancelled(result: AgentAppUpgradeResult | null) {
    if (result) {
      // Profile already committed via the upgrade step: let the caller refresh.
      this.closeOrEmit(result);
    } else if (this.dialogRef) {
      this.dialogRef.close(null);
    } else {
      this.cancelled.emit();
    }
  }

  onBack() {
    if (this.dialogRef) {
      this.dialogRef.close(null);
    }
  }

  private closeOrEmit(result: AgentAppInstallWizardResult) {
    if (this.dialogRef) {
      this.dialogRef.close(result);
    } else {
      this.finished.emit(result);
    }
  }
}
