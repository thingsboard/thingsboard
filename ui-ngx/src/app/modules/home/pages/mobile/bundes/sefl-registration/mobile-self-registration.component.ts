// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  booleanAttribute,
  Component,
  forwardRef,
  Input,
  OnChanges,
  Renderer2,
  SimpleChanges,
  ViewContainerRef
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { EditorPanelComponent } from '@home/pages/mobile/common/editor-panel.component';
import { EntityType } from '@shared/models/entity-type.models';
import { CMAssigneeType, CMScope } from '@app/shared/models/custom-menu.models';
import { GroupPermission } from '@shared/models/group-permission.models';
import {
  CaptchaVersion,
  defaultSignUpFields,
  MobileSelfRegistrationParams,
  SelfRegistrationType,
  SignUpFieldMap
} from '@shared/models/self-register.models';
import { TranslateService } from '@ngx-translate/core';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { NotificationTarget } from '@shared/models/notification.models';
import { MatDialog } from '@angular/material/dialog';

@Component({
    selector: 'tb-mobile-self-registration',
    templateUrl: './mobile-self-registration.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MobileSelfRegistrationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MobileSelfRegistrationComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MobileSelfRegistrationComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input({transform: booleanAttribute})
  androidApp = false;

  @Input({transform: booleanAttribute})
  iOSApp = false;

  selfRegistrationForm = this.fb.group({
    enabled: [false],
    title: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    notificationRecipient: this.fb.control<NotificationTargetId>(null, Validators.required),
    redirect: this.fb.group({
      scheme: ['tbscheme', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
      host: ['app.pe.thingsboard.org', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]]
    }),
    signUpFields: [null],
    captcha: this.fb.group({
      version: this.fb.control<CaptchaVersion>('v3'),
      siteKey: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
      secretKey: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
      logActionName: [''],
      projectId: [{value: '', disabled: true}, Validators.required],
      androidKey: [{value: '', disabled: true}],
      iosKey: [{value: '', disabled: true}],
      serviceAccountCredentials: [{value: '', disabled: true}, Validators.required],
      serviceAccountCredentialsFileName: [{value: '', disabled: true}],
    }),
    showPrivacyPolicy: [true],
    showTermsOfUse: [true],
    termsOfUse: [''],
    privacyPolicy: [''],
    permissions: [([] as Array<GroupPermission>)],
    customerGroupId: [null],
    customerTitlePrefix: [''],
    customMenuId: [null],
    defaultDashboard: this.fb.group({
      id: [null],
      fullscreen: [false]
    }),
    homeDashboard: this.fb.group({
      id: [null],
      hideToolbar: [true]
    })
  });

  EntityType = EntityType;
  CMScope = CMScope;
  CMAssigneeType = CMAssigneeType;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private translate: TranslateService,
              private dialog: MatDialog) {
    this.selfRegistrationForm.get('enabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if(value) {
        this.selfRegistrationForm.enable({emitEvent: false});
        this.updatedCaptchaDisabledState(this.selfRegistrationForm.get('captcha.version').value);
      } else {
        this.selfRegistrationForm.disable({emitEvent: false});
        this.selfRegistrationForm.get('enabled').enable({emitEvent: false});
      }
    });

    this.selfRegistrationForm.get('captcha.version').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((version) => {
      this.updatedCaptchaDisabledState(version);
    })

    this.selfRegistrationForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updateModel());
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'androidApp') {
          if (this.androidApp) {
            this.selfRegistrationForm.get('captcha.androidKey').addValidators(Validators.required);
          } else {
            this.selfRegistrationForm.get('captcha.androidKey').clearValidators();
          }
          this.selfRegistrationForm.get('captcha.androidKey').updateValueAndValidity();
        } else if (propName === 'iOSApp') {
          if (this.androidApp) {
            this.selfRegistrationForm.get('captcha.iosKey').addValidators(Validators.required);
          } else {
            this.selfRegistrationForm.get('captcha.iosKey').clearValidators();
          }
          this.selfRegistrationForm.get('captcha.iosKey').updateValueAndValidity();
        }
      }
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.selfRegistrationForm.disable({emitEvent: false});
    } else {
      this.selfRegistrationForm.get('enabled').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(params: MobileSelfRegistrationParams) {
    if (!isDefinedAndNotNull(params)) {
      if (params === null) {
        params = {} as MobileSelfRegistrationParams;
      }
      params.enabled = true;
      params.signUpFields = deepClone(defaultSignUpFields).map(item => {
        const field = SignUpFieldMap.get(item);
        field.label = this.translate.instant(field.label);
        return field;
      });
    }
    this.selfRegistrationForm.patchValue(params, {emitEvent: false});
    this.selfRegistrationForm.get('enabled').updateValueAndValidity({onlySelf: true});
  }

  validate(): ValidationErrors | null {
    if (!this.selfRegistrationForm.valid) {
      return {
        invalidSelfRegistrationForm: true
      };
    }
    return null;
  }

  editPolicy($event: Event, matButton: MatButton, isPrivacy = false) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        disabled: this.selfRegistrationForm.disabled,
        title: isPrivacy ? 'self-registration.privacy-policy-text' : 'self-registration.terms-of-use-text',
        content: isPrivacy
          ? this.selfRegistrationForm.get('privacyPolicy').value
          : this.selfRegistrationForm.get('termsOfUse').value
      };
      const editorPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EditorPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftBottomOnly', 'leftTopOnly'],
        context: ctx,
        showCloseButton: false,
        popoverContentStyle: {padding: '16px 24px'},
        isModal: true
      });
      editorPanelPopover.tbComponentRef.instance.popover = editorPanelPopover;
      editorPanelPopover.tbComponentRef.instance.editorContentApplied.subscribe((content) => {
        editorPanelPopover.hide();
        if (isPrivacy) {
          this.selfRegistrationForm.get('privacyPolicy').setValue(content);
          this.selfRegistrationForm.get('privacyPolicy').markAsDirty();
        } else {
          this.selfRegistrationForm.get('termsOfUse').setValue(content);
          this.selfRegistrationForm.get('termsOfUse').markAsDirty();
        }
      });
    }
  }

  createTarget(name?: string) {
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        name
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.selfRegistrationForm.get('notificationRecipient').setValue(res.id);
        }
      })
  }

  private updateModel() {
    const value =  this.selfRegistrationForm.getRawValue() as MobileSelfRegistrationParams;
    value.type = SelfRegistrationType.MOBILE;
    this.propagateChange(value);
  }

  private updatedCaptchaDisabledState(version: CaptchaVersion) {
    if (version === 'enterprise') {
      this.selfRegistrationForm.get('captcha.siteKey').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.secretKey').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.projectId').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.androidKey').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.iosKey').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.serviceAccountCredentials').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.serviceAccountCredentialsFileName').enable({emitEvent: false});
    } else {
      this.selfRegistrationForm.get('captcha.siteKey').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.secretKey').enable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.projectId').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.androidKey').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.iosKey').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.serviceAccountCredentials').disable({emitEvent: false});
      this.selfRegistrationForm.get('captcha.serviceAccountCredentialsFileName').disable({emitEvent: false});
    }
  }
}
