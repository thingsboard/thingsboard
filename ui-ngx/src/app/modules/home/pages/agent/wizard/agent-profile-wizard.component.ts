// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Inject, Input, OnInit, Optional, Output, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AgentAppProfile,
  AgentApplicationType,
  AgentProfile,
  AgentProvisionType,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';

export interface AgentProfileWizardData {
  defaults?: Partial<AgentProfile>;
  lockedAppType?: AgentApplicationType;
  // When set, the wizard edits this profile instead of creating a new one.
  profile?: AgentProfile;
}

@Component({
  selector: 'tb-agent-profile-wizard',
  templateUrl: './agent-profile-wizard.component.html',
  styleUrls: ['./agent-profile-wizard.component.scss'],
  standalone: false
})
export class AgentProfileWizardComponent implements OnInit {

  @Input() embedded = false;
  @Input() showBack = false;
  @Input() defaults: Partial<AgentProfile> | null = null;
  @Input() lockedAppType: AgentApplicationType | null = null;

  @Output() finished = new EventEmitter<AgentProfile>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() backClicked = new EventEmitter<void>();

  readonly AgentProvisionType = AgentProvisionType;
  agentProvisionTypes = Object.values(AgentProvisionType);
  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  isEdit = false;

  step1Form: UntypedFormGroup;
  step2Form: UntypedFormGroup;

  submitting = false;
  assignmentsLoadState: 'loading' | 'loaded' | 'failed' = 'loaded';

  @ViewChild('stepper', { static: false }) stepper: MatStepper;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  readonly canCreateAppProfile: boolean;

  constructor(private agentService: AgentService,
              private userPermissionsService: UserPermissionsService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver,
              @Optional() @Inject(MAT_DIALOG_DATA) public data: AgentProfileWizardData | null,
              @Optional() public dialogRef: MatDialogRef<AgentProfileWizardComponent, AgentProfile> | null) {
    this.canCreateAppProfile = this.userPermissionsService.hasGenericPermission(Resource.AGENT_APP_PROFILE, Operation.CREATE);
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
    if (this.data) {
      this.defaults = this.data.defaults ?? null;
      this.lockedAppType = this.data.lockedAppType ?? null;
    }
  }

  ngOnInit() {
    this.isEdit = !!this.data?.profile?.id;
    const defaults = this.data?.profile ?? this.defaults ?? {};
    this.step1Form = this.fb.group({
      name: [defaults.name ?? '', [Validators.required, Validators.maxLength(255)]],
      description: [defaults.description ?? ''],
      provisionType: [defaults.provisionType ?? AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE]
    });
    this.step2Form = this.fb.group({
      appProfileIds: [[]]
    });
    if (this.isEdit) {
      // Keep the picker disabled until the current assignments load, so the user can't edit
      // (and silently lose) an assignment set that hasn't been shown yet.
      this.step2Form.get('appProfileIds').disable({ emitEvent: false });
      this.loadAssignedAppProfiles();
    }
  }

  loadAssignedAppProfiles() {
    this.assignmentsLoadState = 'loading';
    this.agentService.getAgentProfileAppProfileInfos(this.data.profile.id.id).subscribe({
      next: infos => {
        this.step2Form.get('appProfileIds').setValue(infos.map(info => info.id.id));
        this.step2Form.get('appProfileIds').enable({ emitEvent: false });
        this.assignmentsLoadState = 'loaded';
      },
      error: () => {
        this.assignmentsLoadState = 'failed';
      }
    });
  }

  cancel() {
    if (this.dialogRef) {
      this.dialogRef.close(undefined);
    } else {
      this.cancelled.emit();
    }
  }

  back() {
    this.backClicked.emit();
  }

  canSubmit(): boolean {
    return this.step1Form.valid && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;
    const profile: AgentProfile = { ...(this.data?.profile ?? {}), ...this.step1Form.value };
    // In edit mode the picker stays disabled until current assignments load (or the load fails),
    // so this branch is a backstop: an absent appProfileIds param leaves assignments untouched.
    const appProfileIds: string[] = this.isEdit && this.assignmentsLoadState !== 'loaded'
      ? undefined
      : (this.step2Form.value?.appProfileIds ?? []);
    this.agentService.saveAgentProfile(profile, undefined, appProfileIds).subscribe({
      next: saved => {
        if (this.dialogRef) {
          this.dialogRef.close(saved);
        } else {
          this.finished.emit(saved);
        }
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  createAppProfile() {
    this.openAppProfileWizard();
  }

  private openAppProfileWizard() {
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { lockedAppType: this.lockedAppType }
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        const currentIds: string[] = this.step2Form.value?.appProfileIds ?? [];
        if (!currentIds.includes(saved.id.id)) {
          this.step2Form.get('appProfileIds').setValue([...currentIds, saved.id.id]);
        }
      }
    });
  }
}
