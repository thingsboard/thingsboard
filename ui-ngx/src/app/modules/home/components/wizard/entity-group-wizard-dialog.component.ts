// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { MatStepper } from '@angular/material/stepper';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of, Subscription } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { ErrorStateMatcher } from '@angular/material/core';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { EntityGroupInfo, ShareGroupRequest } from '@shared/models/entity-group.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityId } from '@shared/models/id/entity-id';

export interface EntityGroupWizardDialogData {
  ownerId?: EntityId;
  groupName?: string;
  groupType: EntityType;
}

export interface EntityGroupWizardDialogResult {
  entityGroup: EntityGroupInfo;
  shared: boolean;
}

@Component({
    selector: 'tb-entity-group-wizard',
    templateUrl: './entity-group-wizard-dialog.component.html',
    providers: [],
    styleUrls: ['./entity-group-wizard-dialog.component.scss'],
    standalone: false
})
export class EntityGroupWizardDialogComponent extends
  DialogComponent<EntityGroupWizardDialogComponent, EntityGroupWizardDialogResult> implements OnDestroy, ErrorStateMatcher {

  @ViewChild('addEntityGroupWizardStepper', {static: true}) addEntityGroupWizardStepper: MatStepper;

  resource = Resource;

  operation = Operation;

  selectedIndex = 0;

  showNext = true;

  entityType = EntityType;

  entityGroupWizardFormGroup: UntypedFormGroup;

  shareEntityGroupFormGroup: UntypedFormGroup;

  labelPosition = 'end';

  private subscriptions: Subscription[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupWizardDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityGroupWizardDialogComponent, EntityGroupWizardDialogResult>,
              private entityGroupService: EntityGroupService,
              private userPermissionService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.entityGroupWizardFormGroup = this.fb.group({
        name: [this.data.groupName, [Validators.required, Validators.maxLength(255)]],
        description: ['']
      }
    );

    const shareGroupRequest: ShareGroupRequest = {
      ownerId: null,
      allUserGroup: true,
      readElseWrite: true
    };

    this.shareEntityGroupFormGroup = this.fb.group({
      shareEntityGroup: [false],
      shareGroupRequest: [shareGroupRequest, Validators.required]
    });

    this.subscriptions.push(this.shareEntityGroupFormGroup.get('shareEntityGroup').valueChanges.subscribe(
      (shareEntityGroup: boolean) => {
        if (shareEntityGroup) {
          this.shareEntityGroupFormGroup.get('shareGroupRequest').setValidators(Validators.required);
        } else {
          this.shareEntityGroupFormGroup.get('shareGroupRequest').clearValidators();
        }
        this.shareEntityGroupFormGroup.get('shareGroupRequest').updateValueAndValidity();
      }
    ));

    this.labelPosition = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']) ? 'end' : 'bottom';

    this.subscriptions.push(this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.labelPosition = 'end';
          } else {
            this.labelPosition = 'bottom';
          }
        }
      ));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addEntityGroupWizardStepper.previous();
  }

  nextStep(): void {
    this.addEntityGroupWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'entity-group.entity-group-details';
      case 1:
        return 'entity-group.share';
    }
  }

  get maxStepperIndex(): number {
    return this.addEntityGroupWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.allValid()) {
      this.createEntityGroup().pipe(
        mergeMap(entityGroup => this.shareEntityGroup(entityGroup).pipe(
            map((shared) => ({entityGroup, shared} as EntityGroupWizardDialogResult)
            )
          )
        )
      ).subscribe(
        (entityGroup) => {
          this.dialogRef.close(entityGroup);
        }
      );
    }
  }

  private createEntityGroup(): Observable<EntityGroupInfo> {
    const entityGroup = {
      name: this.entityGroupWizardFormGroup.get('name').value.trim(),
      additionalInfo: {
        description: this.entityGroupWizardFormGroup.get('description').value.trim()
      },
      type: this.data.groupType
    } as EntityGroupInfo;
    if (this.data.ownerId) {
      entityGroup.ownerId = this.data.ownerId;
    } else {
      entityGroup.ownerId = this.userPermissionService.getUserOwnerId();
    }
    let saveEntity$: Observable<EntityGroupInfo>;
    if (entityGroup.type === EntityType.DEVICE) {
      saveEntity$ = this.entityGroupService.saveDeviceEntityGroup(entityGroup);
    } else {
      saveEntity$ = this.entityGroupService.saveEntityGroup(entityGroup);
    }
    return saveEntity$;
  }

  private shareEntityGroup(entityGroup: EntityGroupInfo): Observable<boolean> {
    const shareEntityGroup: boolean = this.shareEntityGroupFormGroup.get('shareEntityGroup').value;
    if (shareEntityGroup) {
      const shareGroupRequest = this.shareEntityGroupFormGroup.get('shareGroupRequest').value;
      return this.entityGroupService.shareEntityGroup(entityGroup.id.id, shareGroupRequest).pipe(
        map(() => true
      ));
    } else {
      return of(false);
    }
  }

  allValid(): boolean {
    if (this.addEntityGroupWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addEntityGroupWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    } )) {
      return false;
    } else {
      return true;
    }
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.showNext = false;
    } else {
      this.showNext = true;
    }
  }
}
