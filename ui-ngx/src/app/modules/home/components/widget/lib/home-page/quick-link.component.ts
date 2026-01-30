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
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  SkipSelf,
  ViewChild
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ErrorStateMatcher } from '@angular/material/core';
import { MenuService } from '@core/services/menu.service';
import { Observable, of } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { TranslateService } from '@ngx-translate/core';
import { catchError, distinctUntilChanged, map, publishReplay, refCount, share, switchMap, tap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { deepClone } from '@core/utils';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';

@Component({
    selector: 'tb-quick-link',
    templateUrl: './quick-link.component.html',
    styleUrls: ['./link.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => QuickLinkComponent),
            multi: true
        },
        { provide: ErrorStateMatcher, useExisting: QuickLinkComponent }
    ],
    standalone: false
})
export class QuickLinkComponent extends PageComponent implements OnInit, ControlValueAccessor, ErrorStateMatcher {

  @Input()
  disabled: boolean;

  @Input()
  addOnly = false;

  @Input()
  disableEdit = false;

  @Output()
  quickLinkAdded = new EventEmitter<string>();

  @Output()
  quickLinkAddCanceled = new EventEmitter<void>();

  @Output()
  quickLinkUpdated = new EventEmitter<string>();

  @Output()
  quickLinkDeleted = new EventEmitter<void>();

  @Output()
  editModeChanged = new EventEmitter<boolean>();

  @ViewChild('linkInput', {static: false}) linkInput: ElementRef;

  filteredLinks: Observable<Array<MenuSection>>;

  private allLinksObservable$: Observable<Array<MenuSection>> = null;

  searchText = '';

  editMode = false;
  addMode = false;

  quickLink: MenuSection;

  private propagateChange = null;

  public editQuickLinkFormGroup: UntypedFormGroup;

  private submitted = false;

  private dirty = false;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private menuService: MenuService,
              public translate: TranslateService,
              private customTranslate: CustomTranslatePipe,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store);
  }

  ngOnInit(): void {
    this.addMode = this.addOnly;
    this.editQuickLinkFormGroup = this.fb.group({
      link: [null, [this.requiredLinkValidator]]
    });
    this.filteredLinks = this.editQuickLinkFormGroup.get('link').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value :
          ((value as any).translated ? value.name
            : value.customTranslate ? this.customTranslate.transform(value.name) : this.translate.instant(value.name))) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchLinks(name) ),
        share()
      );
  }

  requiredLinkValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value || typeof value === 'string') {
      return {required: true};
    }
    return null;
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.editQuickLinkFormGroup.disable({emitEvent: false});
    } else {
      this.editQuickLinkFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    if (value) {
      this.menuService.menuLinkById(value).subscribe(
        (link) => {
          this.quickLink = link;
          this.editQuickLinkFormGroup.get('link').patchValue(
            link, {emitEvent: false}
          );
        }
      );
    } else {
      this.quickLink = null;
      this.editQuickLinkFormGroup.get('link').patchValue(
        value, {emitEvent: false}
      );
      if (!this.editQuickLinkFormGroup.valid) {
        this.addMode = true;
        this.editModeChanged.emit(true);
      }
    }
    this.dirty = true;
  }

  updateView(value: MenuSection | null) {
    if (this.quickLink !== value) {
      this.quickLink = value;
    }
  }

  displayLinkFn = (link?: MenuSection): string | undefined =>
    link ? ((link as any).translated ? link.name : link.customTranslate ? this.customTranslate.transform(link.fullName || link.name)
      : this.translate.instant(link.fullName || link.name)) : undefined;

  fetchLinks(searchText?: string): Observable<Array<MenuSection>> {
    this.searchText = searchText;
    const pageLink = new PageLink(100, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getLinks(pageLink).pipe(
      catchError(() => of(emptyPageData<MenuSection>())),
      map(pageData => pageData.data)
    );
  }

  getLinks(pageLink: PageLink): Observable<PageData<MenuSection>> {
    return this.allLinks().pipe(
      map((links) => pageLink.filterData(links))
    );
  }

  allLinks(): Observable<Array<MenuSection>> {
    if (this.allLinksObservable$ === null) {
      this.allLinksObservable$ = this.menuService.availableMenuLinks().pipe(
        map((links) => {
          const result = deepClone(links);
          for (const link of result) {
            link.name = link.customTranslate ? this.customTranslate.transform(link.fullName || link.name)
              : this.translate.instant(link.fullName || link.name);
            (link as any).translated = true;
          }
          return result;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.allLinksObservable$;
  }

  onFocus() {
    if (this.dirty) {
      this.editQuickLinkFormGroup.get('link').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.editQuickLinkFormGroup.get('link').patchValue('');
    setTimeout(() => {
      this.linkInput.nativeElement.blur();
      this.linkInput.nativeElement.focus();
    }, 0);
  }

  switchToEditMode() {
    if (!this.disableEdit && !this.editMode) {
      this.submitted = false;
      this.editQuickLinkFormGroup.get('link').patchValue(
        this.quickLink, {emitEvent: false}
      );
      this.editMode = true;
      this.editModeChanged.emit(true);
    }
  }

  apply() {
    this.submitted = true;
    this.updateModel();
    if (this.quickLink) {
      this.editMode = false;
      this.editModeChanged.emit(false);
      this.quickLinkUpdated.next(this.quickLink.id);
    }
  }

  cancelEdit() {
    this.submitted = false;
    this.editMode = false;
    this.editModeChanged.emit(false);
  }

  add() {
    this.submitted = true;
    this.updateModel();
    if (this.quickLink) {
      if (!this.addOnly) {
        this.addMode = false;
        this.editModeChanged.emit(false);
      }
      this.quickLinkAdded.next(this.quickLink.id);
    }
  }

  cancelAdd() {
    this.editModeChanged.emit(false);
    this.quickLinkAddCanceled.emit();
  }

  delete() {
    this.quickLinkDeleted.emit();
  }

  isEditing() {
    return this.editMode || this.addMode;
  }

  private updateModel() {
    if (this.quickLink) {
      this.propagateChange(this.quickLink.id);
    } else {
      this.propagateChange(null);
    }
  }

}
