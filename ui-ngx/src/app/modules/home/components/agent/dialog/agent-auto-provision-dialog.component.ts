// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { MatStepper } from '@angular/material/stepper';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, share, switchMap, tap } from 'rxjs/operators';
import {
  AgentApplicationType,
  AgentProfile,
  AgentProfileInfo,
  AgentProvisionType,
  agentProvisionTypeSupportsAppAutoInstall
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { AgentProfileCreateDialogService } from '@home/pages/agent/agent-profile-create-dialog.service';
import {
  AgentProfileAssignProfileDialogComponent,
  AgentProfileAssignProfileDialogData
} from '@home/pages/agent/dialog/agent-profile-assign-profile-dialog.component';

type ValidationState = 'idle' | 'validating' | 'valid' | 'invalid-strategy' | 'invalid-no-edge-app';

export interface AgentAutoProvisionDialogData {
  appType?: AgentApplicationType;
}

@Component({
  selector: 'tb-agent-auto-provision-dialog',
  templateUrl: './agent-auto-provision-dialog.component.html',
  styleUrls: ['./agent-auto-provision-dialog.component.scss'],
  standalone: false
})
export class AgentAutoProvisionDialogComponent
  extends DialogComponent<AgentAutoProvisionDialogComponent, boolean> {

  selectForm: UntypedFormGroup;

  filteredProfiles$: Observable<AgentProfileInfo[]>;

  selectedProfile: AgentProfile | null = null;

  validation: ValidationState = 'idle';

  searchText = '';
  loading = false;

  @ViewChild('stepper', { static: false }) stepper: MatStepper;
  @ViewChild(MatAutocompleteTrigger) private autocompleteTrigger: MatAutocompleteTrigger;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  get appType(): AgentApplicationType | null {
    return this.data?.appType ?? null;
  }

  private get i18nPrefix(): string {
    if (!this.appType) {
      return 'agent';
    }
    return this.appType === AgentApplicationType.GATEWAY ? 'gateway' : 'edge';
  }

  get titleKey(): string { return `${this.i18nPrefix}.auto-provision-title`; }
  get infoTitleKey(): string { return `${this.i18nPrefix}.auto-provision-info-title`; }
  get infoBodyKey(): string { return `${this.i18nPrefix}.auto-provision-info-body`; }
  get stepSelectKey(): string { return `${this.i18nPrefix}.auto-provision-step-select`; }
  get stepScriptKey(): string { return `${this.i18nPrefix}.auto-provision-step-script`; }
  get validKey(): string { return `${this.i18nPrefix}.auto-provision-valid`; }
  get invalidNoAppKey(): string { return `${this.i18nPrefix}.auto-provision-invalid-no-edge-app-profile`; }
  get successMessageKey(): string { return `${this.i18nPrefix}.auto-provision-success-message`; }
  get scriptHintKey(): string { return `${this.i18nPrefix}.auto-provision-script-hint`; }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private agentProfileCreateDialogService: AgentProfileCreateDialogService,
              private breakpointObserver: BreakpointObserver,
              private destroyRef: DestroyRef,
              @Inject(MAT_DIALOG_DATA) public data: AgentAutoProvisionDialogData,
              public dialogRef: MatDialogRef<AgentAutoProvisionDialogComponent, boolean>) {
    super(store, router, dialogRef);

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));

    this.selectForm = this.fb.group({
      agentProfile: [null]
    });

    this.filteredProfiles$ = this.selectForm.get('agentProfile').valueChanges.pipe(
      tap(value => {
        if (!value || typeof value === 'string') {
          this.selectedProfile = null;
          this.validation = 'idle';
        }
      }),
      map(value => (!value ? '' : (typeof value === 'string' ? value : value.name))),
      debounceTime(150),
      distinctUntilChanged(),
      switchMap(name => this.fetchEligibleProfiles(name)),
      share()
    );
  }

  onProfileFocus() {
    this.selectForm.get('agentProfile').updateValueAndValidity({ onlySelf: true, emitEvent: true });
  }

  fetchEligibleProfiles(searchText?: string): Observable<AgentProfileInfo[]> {
    this.searchText = searchText;
    // Fetch a generous page and filter client-side. Eligible = provisionType auto-installs apps.
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.agentService.getTenantAgentProfileInfos(pageLink, { ignoreLoading: true }).pipe(
      catchError(() => of(emptyPageData<AgentProfileInfo>())),
      map(page => (page.data || []).filter(p =>
        agentProvisionTypeSupportsAppAutoInstall(p.provisionType)
      ))
    );
  }

  displayProfile(profile?: AgentProfileInfo): string {
    return profile ? profile.name : '';
  }

  onProfileSelected(profile: AgentProfileInfo) {
    if (!profile) {
      return;
    }
    this.validateProfile(profile);
  }

  private validateProfile(profile: AgentProfileInfo) {
    this.selectedProfile = profile;

    if (!agentProvisionTypeSupportsAppAutoInstall(profile.provisionType)) {
      this.validation = 'invalid-strategy';
      return;
    }

    if (!this.appType) {
      this.validation = 'valid';
      return;
    }

    this.validation = 'validating';

    this.agentService.getAgentProfileAppProfileInfos(profile.id.id, { ignoreLoading: true }).pipe(
      map(profiles => (profiles || []).some(p => p?.appType === this.appType)),
      catchError(() => of(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(hasEdge => {
      if (this.selectedProfile?.id?.id !== profile.id.id) {
        return; // stale response, user has moved on
      }
      this.validation = hasEdge ? 'valid' : 'invalid-no-edge-app';
    });
  }

  canProceed(): boolean {
    return this.validation === 'valid' && !!this.selectedProfile;
  }

  createNewProfile($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.autocompleteTrigger?.closePanel();
    this.agentProfileCreateDialogService.open({
      defaults: { provisionType: AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE },
      lockedAppType: this.appType
    }).pipe(
      filter(saved => !!saved),
      switchMap(saved => this.agentService.getAgentProfileInfoById(saved.id.id)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(profile => {
      this.selectForm.get('agentProfile').setValue(profile, { emitEvent: false });
      this.validateProfile(profile);
    });
  }

  openSelectedProfile() {
    if (!this.selectedProfile?.id?.id) {
      return;
    }
    this.router.navigateByUrl(`/edgeManagement/profiles/agent/${this.selectedProfile.id.id}`);
    this.dialogRef.close(true);
  }

  assignProfileToSelected($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const profileId = this.selectedProfile?.id?.id;
    if (!profileId) {
      return;
    }
    this.agentService.getAgentProfileAppProfileInfos(profileId, { ignoreLoading: true }).pipe(
      switchMap(profiles => {
        const selectedIds = (profiles || []).map(p => p.id.id);
        return this.dialog.open<AgentProfileAssignProfileDialogComponent, AgentProfileAssignProfileDialogData, string[]>(
          AgentProfileAssignProfileDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog'],
            data: { selectedIds, lockedAppType: this.appType }
          }
        ).afterClosed();
      }),
      filter(nextIds => nextIds !== null && nextIds !== undefined),
      switchMap(nextIds => this.agentService.assignAppProfilesToAgentProfile(profileId, nextIds)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      if (this.selectedProfile) {
        this.validateProfile(this.selectedProfile as AgentProfileInfo);
      }
    });
  }

  dockerCommand = '';

  proceedToScript() {
    if (!this.canProceed()) {
      return;
    }
    // Refetch the full profile (with provisionKey/Secret) and load the resolved install command.
    this.loading = true;
    this.agentService.getAgentProfileById(this.selectedProfile.id.id).pipe(
      tap(full => this.selectedProfile = full),
      switchMap(full => this.agentService.getAgentProvisionInstructions(full.id.id).pipe(
        catchError(() => of({ instructions: '' }))
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: res => {
        this.dockerCommand = res?.instructions || '';
        this.loading = false;
        this.stepper.next();
      },
      error: () => { this.loading = false; }
    });
  }

  hasCredentials(): boolean {
    return !!(this.selectedProfile?.provisionKey && this.selectedProfile?.provisionSecret);
  }

  onCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.install-command-copied-message'),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  cancel() {
    this.dialogRef.close(false);
  }

  goToProfile() {
    this.openSelectedProfile();
  }
}
