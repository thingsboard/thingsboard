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
import { PageComponent } from '@shared/components/page.component';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import {
  SecretStorageData,
  SecretStorageDialogComponent
} from '@shared/components/secret-storage/secret-storage-dialog.component';
import { parseSecret, SecretStorageType } from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { coerceNumber } from '@shared/decorators/coercion';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-secret-key-input',
    templateUrl: './secret-key-input.component.html',
    styleUrls: ['./secret-key-input.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SecretKeyInputComponent),
            multi: true
        }
    ],
    standalone: false
})
export class SecretKeyInputComponent extends PageComponent implements OnInit, ControlValueAccessor, OnChanges {

  @Input()
  label: string;

  @Input()
  requiredText: string;

  @Input()
  hint: string;

  @Input({transform: booleanAttribute})
  required: boolean = false;

  @Input()
  disabled: boolean;

  @Input({transform: booleanAttribute})
  readonly = false;

  @Input()
  @coerceNumber()
  maxLength: number;

  @Input()
  maxLengthErrorText: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  /** Native input type. Defaults to 'password' (masked + toggle-visibility button). Set to 'text'
   *  for fields that are sensitive only in the sense that they may be backed by a Secret reference,
   *  but whose plaintext form is not actually a secret (e.g. Application ID). */
  @Input()
  inputType: 'text' | 'password' = 'password';

  secretStorageKey: string;

  allowSecret = getCurrentAuthUser(this.store).authority !== Authority.CUSTOMER_USER;

  private modelValue: string;

  private propagateChange = null;

  public secretKeyFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private secretStorageService: SecretStorageService,
              private userPermissionsService: UserPermissionsService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.readonly = this.readonly || !this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.WRITE);
    const validators = [Validators.pattern(/.*\S.*/)];
    if (this.required) {
      validators.push(Validators.required);
    }
    if (this.maxLength && this.maxLengthErrorText) {
      validators.push(Validators.maxLength(this.maxLength));
    }
    this.secretKeyFormGroup = this.fb.group({
      secretKey: [null, validators]
    });

    this.secretKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.required) {
      const requiredChanges = changes.required;
      if (!requiredChanges.firstChange && requiredChanges.currentValue !== requiredChanges.previousValue) {
        this.updateValidators();
      }
    }
    if (changes.maxLength) {
      const requiredChanges = changes.maxLength;
      if (!requiredChanges.firstChange && requiredChanges.currentValue !== requiredChanges.previousValue) {
        this.updateValidators();
      }
    }
  }

  private updateValidators() {
    if (this.secretKeyFormGroup) {
      const validators = [];
      if (this.required) {
        validators.push(Validators.required);
      }
      if (this.maxLength && this.maxLengthErrorText) {
        validators.push(Validators.maxLength(this.maxLength));
      }
      this.secretKeyFormGroup.get('secretKey').setValidators(validators);
      this.secretKeyFormGroup.get('secretKey').updateValueAndValidity();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.secretKeyFormGroup.disable({emitEvent: false});
    } else {
      this.secretKeyFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    const parsedSecret = parseSecret(value);
    this.modelValue = value;
    if (parsedSecret) {
      this.secretStorageService.getSecretByName(parsedSecret, {ignoreErrors: true}).subscribe({
        next: () => {
          this.secretStorageKey = parsedSecret;
          this.secretKeyFormGroup.patchValue(
            {secretKey: this.modelValue}, {emitEvent: false}
          );
        },
        error: () => {
          this.secretStorageKey = null;
          this.secretKeyFormGroup.patchValue(
            {secretKey: null}, {emitEvent: true}
          );
          this.secretKeyFormGroup.get('secretKey').markAsTouched();
        }
      })
    } else {
      this.secretStorageKey = null;
      this.secretKeyFormGroup.patchValue(
        { secretKey: this.modelValue }, {emitEvent: false}
      );
    }
  }

  private updateModel() {
    const secretKey: string = this.secretKeyFormGroup.get('secretKey').value;
    this.secretStorageKey = parseSecret(secretKey);
    if (this.modelValue !== secretKey) {
      this.modelValue = secretKey;
      this.propagateChange(this.modelValue);
    }
  }

  remove($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.secretKeyFormGroup.get('secretKey').patchValue(null, {emitEvent: true});
    this.secretKeyFormGroup.get('secretKey').markAsTouched();
  }

  openSecretKeyDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SecretStorageDialogComponent, SecretStorageData, string>(SecretStorageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        type: SecretStorageType.TEXT,
        value: this.secretKeyFormGroup.get('secretKey').value,
        hideType: true
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.secretKeyFormGroup.get('secretKey').patchValue(res, {emitEvent: true});
        }
      });
  }
}
