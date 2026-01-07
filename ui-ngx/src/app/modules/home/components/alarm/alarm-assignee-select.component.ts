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

import { Component, forwardRef, Injector, Input, OnInit, StaticProvider, ViewContainerRef } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { UserId } from '@shared/models/id/user-id';
import { UserService } from '@core/http/user.service';
import { User, UserEmailInfo } from '@shared/models/user.model';
import { catchError, map, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  ALARM_ASSIGNEE_SELECT_PANEL_DATA,
  AlarmAssigneeSelectPanelComponent,
  AlarmAssigneeSelectPanelData
} from '@home/components/alarm/alarm-assignee-select-panel.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { AlarmAssigneeOption } from '@shared/models/alarm.models';

@Component({
  selector: 'tb-alarm-assignee-select',
  templateUrl: './alarm-assignee-select.component.html',
  styleUrls: ['./alarm-assignee.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmAssigneeSelectComponent),
      multi: true
    }
  ]
})
export class AlarmAssigneeSelectComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  inline = false;

  @coerceBoolean()
  @Input()
  userMode = false;

  assigneeFormGroup: UntypedFormGroup;
  assignee?: User | UserEmailInfo;
  assigneeOption?: AlarmAssigneeOption;

  private propagateChange = (_: any) => {};

  constructor(private utilsService: UtilsService,
              private overlay: Overlay,
              private fb: UntypedFormBuilder,
              private userService: UserService,
              private viewContainerRef: ViewContainerRef,
              private translateService: TranslateService) {
  }

  ngOnInit(): void {
    this.assigneeFormGroup = this.fb.group({
      assignee: [null, []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.assigneeFormGroup.disable({emitEvent: false});
    } else {
      this.assigneeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value?: UserId | AlarmAssigneeOption): void {
    let userId: UserId;
    if (value && (value as UserId).id) {
      userId = value as UserId;
      this.assigneeOption = null;
    } else {
      userId = null;
      this.assigneeOption = value ? value as AlarmAssigneeOption : AlarmAssigneeOption.noAssignee;
    }
    const userObservable = userId ? this.userService.getUser(userId.id, {ignoreErrors: true}).pipe(
      catchError(() => of(null))
    ) : of(null);
    userObservable.pipe(
      tap((user) => {
        this.assignee = user;
      }),
      map((user) => this.getAssignee(user))
    ).subscribe((assignee) => {
      if (assignee) {
        this.assigneeFormGroup.get('assignee').patchValue(assignee, {emitEvent: false});
      } else {
        if (!this.assigneeOption) {
          this.assigneeOption = AlarmAssigneeOption.noAssignee;
        }
        assignee = this.getAssigneeOption(this.assigneeOption);
        this.assigneeFormGroup.get('assignee').patchValue(assignee, {emitEvent: false});
      }
    });
  }

  private getAssignee(user?: User| UserEmailInfo): string | null {
    if (user) {
      return this.getUserDisplayName(user);
    } else {
      return null;
    }
  }

  private getAssigneeOption(assigneeOption: AlarmAssigneeOption): string {
    if (assigneeOption === AlarmAssigneeOption.noAssignee) {
      return this.translateService.instant('alarm.assignee-not-set');
    } else {
      return this.translateService.instant(this.userMode ? 'alarm.assigned-to-me' : 'alarm.assigned-to-current-user');
    }
  }

  private getUserDisplayName(user?: User | UserEmailInfo): string {
    let displayName = '';
    if ((user?.firstName && user?.firstName.length > 0) ||
      (user?.lastName && user?.lastName.length > 0)) {
      if (user?.firstName) {
        displayName += user?.firstName;
      }
      if (user?.lastName) {
        if (displayName.length > 0) {
          displayName += ' ';
        }
        displayName += user?.lastName;
      }
    } else {
      displayName = user?.email;
    }
    return displayName;
  }

  getUserInitials(): string {
    let initials = '';
    if (this.assignee?.firstName && this.assignee?.firstName.length ||
      this.assignee?.lastName && this.assignee?.lastName.length) {
      if (this.assignee?.firstName) {
        initials += this.assignee?.firstName.charAt(0);
      }
      if (this.assignee?.lastName) {
        initials += this.assignee?.lastName.charAt(0);
      }
    } else {
      initials += this.assignee?.email.charAt(0);
    }
    return initials.toUpperCase();
  }

  getAvatarBgColor(): string {
    return this.utilsService.stringToHslColor(this.getUserDisplayName(this.assignee), 40, 60);
  }

  openAlarmAssigneeSelectPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.disabled) {
      const target = $event.currentTarget;
      const config = new OverlayConfig();
      config.backdropClass = 'cdk-overlay-transparent-backdrop';
      config.hasBackdrop = true;
      const connectedPosition: ConnectedPosition = {
        originX: 'center',
        originY: 'bottom',
        overlayX: 'center',
        overlayY: 'top'
      };
      config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
        .withPositions([connectedPosition]);
      config.width = (target as HTMLElement).offsetWidth;
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });
      const providers: StaticProvider[] = [
        {
          provide: ALARM_ASSIGNEE_SELECT_PANEL_DATA,
          useValue: {
            assigneeId: this.assignee?.id?.id,
            assigneeOption: this.assigneeOption,
            userMode: this.userMode
          } as AlarmAssigneeSelectPanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      const component = overlayRef.attach(new ComponentPortal(AlarmAssigneeSelectPanelComponent,
        this.viewContainerRef, injector));
      component.onDestroy(() => {
        if (component.instance.userSelected) {
          this.assignee = component.instance.result;
          this.assigneeOption = component.instance.optionResult;
          if (this.assignee) {
            this.assigneeFormGroup.get('assignee').patchValue(this.getAssignee(this.assignee), {emitEvent: false});
            this.propagateChange(this.assignee?.id);
          } else if (this.assigneeOption) {
            this.assigneeFormGroup.get('assignee').patchValue(this.getAssigneeOption(this.assigneeOption), {emitEvent: false});
            this.propagateChange(this.assigneeOption);
          }
        }
      });
    }
  }

}
