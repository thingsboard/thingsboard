// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  booleanAttribute,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CombinedGenericPermissions, GenericRolePermissions } from '@shared/models/role.models';
import { Operation, Resource } from '@shared/models/security.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@app/shared/public-api';

interface PermissionRowForm {
  resource: FormControl<Resource>;
  operations: FormControl<Operation[]>;
  excludedOperations: FormControl<Operation[]>;
}

@Component({
  selector: 'tb-permission-list',
  templateUrl: './permission-list.component.html',
  styleUrls: ['./permission-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PermissionListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PermissionListComponent),
      multi: true
    }
  ],
  standalone: false
})
export class PermissionListComponent extends PageComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Input({transform: booleanAttribute})
  required: boolean;

  isMobile = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);

  permissionsFormArray = this.fb.array<FormGroup<PermissionRowForm>>([], ()=> this.uniqueResourcesValidator());

  private propagateChange: (value: CombinedGenericPermissions) => void;
  private onValidatorChange: () => void;

  get hasRequiredError(): boolean {
    if (!this.required || !this.permissionsFormArray.touched) {
      return false;
    }
    return !this.permissionsFormArray.controls
      .some(row => row.controls.operations.value?.length > 0);
  }

  constructor(protected store: Store<AppState>,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.permissionsFormArray.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.required) {
      this.onValidatorChange?.();
    }
  }

  registerOnChange(fn: (value: CombinedGenericPermissions) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.permissionsFormArray.disable({emitEvent: false});
    } else {
      this.permissionsFormArray.enable({emitEvent: false});
    }
  }

  writeValue(value: CombinedGenericPermissions): void {
    this.permissionsFormArray.clear({emitEvent: false});
    if (value) {
      const permissions = value.permissions || {};
      const excludedPermissions = value.excludedPermissions || {};
      const allResources = new Set([...Object.keys(permissions), ...Object.keys(excludedPermissions)]);
      for (const resource of allResources) {
        const ops = permissions[resource] || null;
        const excludedOps = excludedPermissions[resource] || null;
        const rowGroup = this.buildRowGroup(Resource[resource], ops, excludedOps);
        if (this.disabled) {
          rowGroup.disable();
        }
        this.permissionsFormArray.push(rowGroup, {emitEvent: false});
      }
    }
  }

  removePermission(index: number) {
    this.permissionsFormArray.removeAt(index);
  }

  addPermission() {
    this.permissionsFormArray.push(this.buildRowGroup(null, null, null));
  }

  validate(_control: AbstractControl): ValidationErrors | null {
    if (this.required) {
      const hasIncludedOps = this.permissionsFormArray.controls
        .some(row => row.controls.operations.value?.length > 0);
      if (!hasIncludedOps) {
        return {required: true};
      }
    }
    for (const control of this.permissionsFormArray.controls) {
      if (control.invalid) {
        if (control.controls.resource.hasError('resourceDuplicate')) {
          return {resourceDuplicate: true};
        }
        if (control.hasError('operationsRequired')) {
          return {operationsRequired: true};
        }
        if (control.hasError('operationOverlap')) {
          return {operationOverlap: true};
        }
        if (control.controls.resource?.hasError('required')) {
          return {invalid: true};
        }
      }
    }
    return null;
  }

  private buildRowGroup(resource: Resource, operations: Operation[], excludedOperations: Operation[]): FormGroup<PermissionRowForm> {
    return this.fb.group<PermissionRowForm>({
      resource: this.fb.control<Resource>(resource, Validators.required),
      operations: this.fb.control<Operation[]>(operations),
      excludedOperations: this.fb.control<Operation[]>(excludedOperations)
    }, {validators: [PermissionListComponent.rowValidator]});
  }

  private updateModel() {
    const permissionList = this.permissionsFormArray.getRawValue();
    const permissions: GenericRolePermissions = {};
    const excludedPermissions: GenericRolePermissions = {};
    permissionList.forEach((entry) => {
      if (entry.resource) {
        if (entry.operations?.length) {
          permissions[entry.resource] = entry.operations;
        }
        if (entry.excludedOperations?.length) {
          excludedPermissions[entry.resource] = entry.excludedOperations;
        }
      }
    });
    this.propagateChange?.({permissions, excludedPermissions});
  }

  static rowValidator(control: AbstractControl): ValidationErrors | null {
    const operations: Operation[] = control.get('operations')?.value;
    const excludedOperations: Operation[] = control.get('excludedOperations')?.value;
    const hasOps = operations?.length > 0;
    const hasExcludedOps = excludedOperations?.length > 0;
    if (!hasOps && !hasExcludedOps) {
      return {operationsRequired: true};
    }
    if (hasOps && hasExcludedOps) {
      if (operations.includes(Operation.ALL) && excludedOperations.includes(Operation.ALL)) {
        return {operationOverlap: true};
      }
      if (!operations.includes(Operation.ALL)) {
        const expandDenied = excludedOperations.includes(Operation.ALL)
          ? Object.values(Operation).filter(o => o !== Operation.ALL)
          : excludedOperations;
        const allowedSet = new Set(operations);
        if (expandDenied.some(op => allowedSet.has(op))) {
          return {operationOverlap: true};
        }
      }
    }
    return null;
  }

  private uniqueResourcesValidator(): ValidationErrors | null {
    const seen = new Map<Resource, FormControl>();

    if(this.permissionsFormArray?.controls) {
      this.permissionsFormArray.controls.forEach((control) => {
        const resource = control.controls.resource.value;
        const resourceControl = control.controls.resource;

        if (!resource) return;

        if (seen.has(resource)) {
          resourceControl.setErrors({ ...resourceControl.errors, resourceDuplicate: true });
          resourceControl.markAsTouched();
          resourceControl.updateValueAndValidity({ onlySelf: true, emitEvent: false });
        } else {
          if (resourceControl.hasError('resourceDuplicate')) {
            const { resourceDuplicate, ...rest } = resourceControl.errors;
            resourceControl.setErrors(Object.keys(rest).length ? rest : null);
            resourceControl.updateValueAndValidity({ onlySelf: true, emitEvent: false });
          }
          seen.set(resource, resourceControl);
        }
      });

      const hasDuplicates = this.permissionsFormArray.controls
        .some(c => c.controls.resource.hasError('resourceDuplicate'));
      return hasDuplicates ? { resourceDuplicate: true } : null;
    }
    return null
  }
}
