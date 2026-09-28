// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  booleanAttribute,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
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
import { UtilsService } from '@core/services/utils.service';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-secret-file-input',
    templateUrl: './secret-file-input.component.html',
    styleUrls: ['./secret-file-input.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SecretFileInputComponent),
            multi: true
        }
    ],
    standalone: false
})
export class SecretFileInputComponent extends PageComponent implements OnInit, ControlValueAccessor, OnChanges {

  @Input()
  label: string;

  @Input()
  noFileText: string;

  @Input()
  existingFileName: string;

  @Input()
  inputId = this.utils.guid();

  @Input({transform: booleanAttribute})
  required: boolean = false;

  @Input()
  disabled: boolean;

  @Input({transform: booleanAttribute})
  readonly = false;

  @Output()
  fileNameChanged = new EventEmitter<string|string[]>();

  @Input()
  accept = '*/*';

  @Input()
  maxSizeByte = 524288;

  @Input()
  allowedExtensions: string;

  secretStorageFile: string;

  allowSecret = getCurrentAuthUser(this.store).authority !== Authority.CUSTOMER_USER;

  private modelValue: string;

  private propagateChange = null;

  public secretFileFormGroup: UntypedFormGroup;


  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private secretStorageService: SecretStorageService,
              private userPermissionsService: UserPermissionsService,
              private utils: UtilsService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.readonly = this.readonly || !this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.WRITE);
    this.secretFileFormGroup = this.fb.group({
      secretFile: [null, this.required ? [Validators.required] : []]
    });

    this.secretFileFormGroup.valueChanges.pipe(
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
  }

  private updateValidators() {
    if (this.secretFileFormGroup) {
      this.secretFileFormGroup.get('secretFile').setValidators(this.required ? [Validators.required] : []);
      this.secretFileFormGroup.get('secretFile').updateValueAndValidity();
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
      this.secretFileFormGroup.disable({emitEvent: false});
    } else {
      this.secretFileFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    const parsedSecret = parseSecret(value);
    this.modelValue = value;
    if (parsedSecret) {
      this.secretStorageService.getSecretByName(parsedSecret, {ignoreErrors: true}).subscribe({
        next: () => {
          this.secretStorageFile = parsedSecret;
          this.existingFileName = null;
          this.secretFileFormGroup.patchValue(
            {secretFile: this.modelValue}, {emitEvent: false}
          );
        },
        error: () => {
          this.secretStorageFile = null;
          this.existingFileName = null;
          this.secretFileFormGroup.patchValue(
            {secretFile: null}, {emitEvent: true}
          );
        }
      })
    } else {
      this.secretStorageFile = null;
      this.secretFileFormGroup.patchValue(
        { secretFile: this.modelValue }, {emitEvent: false}
      );
    }
  }

  fileNameChange($event: any) {
    this.existingFileName = $event;
    this.fileNameChanged.emit(this.existingFileName);
  }

  private updateModel() {
    const secretFile: string = this.secretFileFormGroup.get('secretFile').value;
    this.secretStorageFile = parseSecret(secretFile);
    if (this.modelValue !== secretFile) {
      this.modelValue = secretFile;
    }
    this.propagateChange(this.modelValue);
  }

  remove($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.secretFileFormGroup.get('secretFile').patchValue(null, {emitEvent: true});
    this.fileNameChanged.emit('');
    this.existingFileName = null;
  }

  openSecretKeyDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SecretStorageDialogComponent, SecretStorageData, string>(SecretStorageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        type: SecretStorageType.TEXT_FILE,
        value: this.secretFileFormGroup.get('secretFile').value,
        fileName: this.existingFileName,
        hideType: true
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.secretFileFormGroup.get('secretFile').patchValue(res, {emitEvent: true});
        }
      });
  }
}
