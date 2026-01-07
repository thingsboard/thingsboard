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

import {
  AfterViewInit,
  Component,
  ElementRef,
  Inject,
  InjectionToken, OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  share,
  switchMap,
  takeUntil,
} from 'rxjs/operators';
import { User, UserEmailInfo } from '@shared/models/user.model';
import { TranslateService } from '@ngx-translate/core';
import { UserService } from '@core/http/user.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { AlarmService } from '@core/http/alarm.service';
import { OverlayRef } from '@angular/cdk/overlay';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { UtilsService } from '@core/services/utils.service';
import { AlarmAssigneeOption, getUserDisplayName, getUserInitials } from '@shared/models/alarm.models';

export const ALARM_ASSIGNEE_PANEL_DATA = new InjectionToken<any>('AlarmAssigneePanelData');

export interface AlarmAssigneePanelData {
  alarmId: string;
  assigneeId: string;
}

@Component({
  selector: 'tb-alarm-assignee-panel',
  templateUrl: './alarm-assignee-panel.component.html',
  styleUrls: ['./alarm-assignee-panel.component.scss']
})
export class AlarmAssigneePanelComponent implements  OnInit, AfterViewInit, OnDestroy {

  assigneeOptions = AlarmAssigneeOption;

  private dirty = false;

  alarmId: string;

  assigneeId?: string;
  assigneeOption?: AlarmAssigneeOption = null;

  assigneeNotSetText = 'alarm.unassigned';
  assignedToCurrentUserText = '';

  reassigned = false;

  selectUserFormGroup: FormGroup;

  @ViewChild('userInput', {static: true}) userInput: ElementRef;

  filteredUsers: Observable<Array<UserEmailInfo>>;

  searchText = '';

  get displayAssigneeNotSet(): boolean {
    return !!this.assigneeId;
  }

  get displayAssignedToCurrentUser(): boolean {
    return false;
  }

  private destroy$ = new Subject<void>();

  constructor(@Inject(ALARM_ASSIGNEE_PANEL_DATA) public data: AlarmAssigneePanelData,
              public overlayRef: OverlayRef,
              public translate: TranslateService,
              private userService: UserService,
              private alarmService: AlarmService,
              private fb: FormBuilder,
              private utilsService: UtilsService) {
    this.alarmId = data.alarmId;
    this.assigneeId = data.assigneeId;
    this.selectUserFormGroup = this.fb.group({
      user: [null]
    });
  }

  ngOnInit() {
    this.filteredUsers = this.selectUserFormGroup.get('user').valueChanges
      .pipe(
        debounceTime(150),
        map(value => value ? (typeof value === 'string' ? value : '') : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchUsers(name)),
        share(),
        takeUntil(this.destroy$)
    );
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.userInput.nativeElement.focus();
    }, 0);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  displayUserFn(user?: User): string | undefined {
    return user ? user.email : undefined;
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.clear();
    if (event.option.value?.id) {
      const user: User = event.option.value;
      this.assign(user);
    } else {
      this.unassign();
    }
  }

  assign(user: User): void {
    this.alarmService.assignAlarm(this.alarmId, user.id.id, {ignoreLoading: true}).subscribe(
      () => {
        this.reassigned = true;
        this.overlayRef.dispose();
      });
  }

  unassign(): void {
    this.alarmService.unassignAlarm(this.alarmId, {ignoreLoading: true}).subscribe(
      () => {
        this.reassigned = true;
        this.overlayRef.dispose();
      });
  }

  fetchUsers(searchText?: string): Observable<Array<UserEmailInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'email',
      direction: Direction.ASC
    });
    return this.userService.getUsersForAssign(this.alarmId, pageLink, {ignoreLoading: true})
      .pipe(
      catchError(() => of(emptyPageData<UserEmailInfo>())),
      map(pageData => pageData.data)
    );
  }

  onFocus(): void {
    if (!this.dirty) {
      this.selectUserFormGroup.get('user').updateValueAndValidity({onlySelf: true});
      this.dirty = true;
    }
  }

  clear() {
    this.selectUserFormGroup.get('user').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.userInput.nativeElement.blur();
      this.userInput.nativeElement.focus();
    }, 0);
  }

  getUserInitials(entity: UserEmailInfo): string {
    return getUserInitials(entity);
  }

  getFullName(entity: UserEmailInfo): string {
    let fullName = '';
    if ((entity.firstName && entity.firstName.length > 0) ||
      (entity.lastName && entity.lastName.length > 0)) {
      if (entity.firstName) {
        fullName += entity.firstName;
      }
      if (entity.lastName) {
        if (fullName.length > 0) {
          fullName += ' ';
        }
        fullName += entity.lastName;
      }
    }
    return fullName;
  }

  getAvatarBgColor(entity: UserEmailInfo) {
    return this.utilsService.stringToHslColor(getUserDisplayName(entity), 40, 60);
  }

}
