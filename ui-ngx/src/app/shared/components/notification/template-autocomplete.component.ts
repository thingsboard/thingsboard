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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationTemplate,
  NotificationType
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { isEqual, objectRequired } from '@core/utils';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification/template/template-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';

@Component({
    selector: 'tb-template-autocomplete',
    templateUrl: './template-autocomplete.component.html',
    styleUrls: ['./template-autocomplete.component.scss'],
    encapsulation: ViewEncapsulation.None,
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TemplateAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class TemplateAutocompleteComponent extends AutocompleteBaseDirective implements ControlValueAccessor, OnInit {

  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;
  selectTemplateFormGroup: FormGroup;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  allowCreate = false;

  @Input()
  @coerceBoolean()
  allowEdit = false;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  disabled: boolean;

  private notificationTypeValue: NotificationType;
  get notificationTypes(): NotificationType {
    return this.notificationTypeValue;
  }
  @Input()
  set notificationTypes(type) {
    if (type !== this.notificationTypeValue) {
      this.notificationTypeValue = type;
      this.reset();
    }
  }

  @ViewChild('templateInput', {static: true}) templateInput: ElementRef;

  @ViewChild('templateInput', {read: MatAutocompleteTrigger}) autocompleteTrigger: MatAutocompleteTrigger;

  filteredTemplate: Observable<Array<NotificationTemplate>>;

  private modelValue: EntityId | null;

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private notificationService: NotificationService,
              private fb: FormBuilder,
              private dialog: MatDialog) {
    super();
    this.selectTemplateFormGroup = this.fb.group({
      templateName: [null]
    });
  }

  protected getControl(): FormControl {
    return this.selectTemplateFormGroup.get('templateName') as FormControl;
  }

  protected getInput(): ElementRef<HTMLInputElement> {
    return this.templateInput as ElementRef<HTMLInputElement>;
  }

  ngOnInit() {
    const templateControl = this.selectTemplateFormGroup.get('templateName');
    templateControl.addValidators(objectRequired());
    if (this.required) {
      templateControl.addValidators(Validators.required);
    }
    templateControl.updateValueAndValidity({emitEvent: false});
    this.filteredTemplate = this.selectTemplateFormGroup.get('templateName').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        switchMap(name => this.fetchTemplate(name)),
        shareReplay(1)
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectTemplateFormGroup.disable({emitEvent: false});
    } else {
      this.selectTemplateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.searchText = '';
    if (value != null) {
      this.notificationService.getNotificationTemplateById(value.id, {
        ignoreLoading: true,
        ignoreErrors: true
      }).subscribe({
        next: (entity) => {
          this.modelValue = entity.id;
          this.selectTemplateFormGroup.get('templateName').patchValue(entity, {emitEvent: false});
        },
        error: () => {
          this.modelValue = null;
          this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      });
    } else {
      this.modelValue = null;
      this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  displayTemplateFn(template?: NotificationTemplate): string | undefined {
    return template ? template.name : undefined;
  }

  editTemplate($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.getNotificationTemplateById(this.modelValue.id).subscribe(
      (template) => {
        this.openNotificationTemplateDialog({template});
      }
    );
  }

  createTemplate($event: Event, button: MatButton) {
    $event?.stopPropagation();
    button._elementRef.nativeElement.blur();
    this.createTemplateByName($event);
  }

  createTemplateByName($event: Event, name?: string) {
    $event?.stopPropagation();
    this.openNotificationTemplateDialog({
      isAdd: true,
      predefinedType: this.notificationTypes,
      name
    });
  }

  private openNotificationTemplateDialog(dialogData?: TemplateNotificationDialogData) {
    this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData,
      NotificationTemplate>(TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: dialogData
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.selectTemplateFormGroup.get('templateName').patchValue(res);
        }
      });
  }

  updateView(value: EntityId | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchTemplate(searchText?: string): Observable<Array<NotificationTemplate>> {
    this.searchText = searchText ?? '';
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.notificationService.getNotificationTemplates(pageLink, this.notificationTypes, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<NotificationTemplate>())),
      map(pageData => pageData.data)
    );
  }

  override reset() {
    super.reset();
    this.updateView(null);
    this.dirty = true;
  }

  getNotificationDeliveryMethodInfoMap(key: string) {
    return this.notificationDeliveryMethodInfoMap.get(key as NotificationDeliveryMethod);
  }
}
