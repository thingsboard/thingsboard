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

import { Directive, ElementRef, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { first, takeUntil } from 'rxjs/operators';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { FormControl } from '@angular/forms';

interface BaseType {
  name?: string;
  title?: string;
  id?: string | { id: string };
}

@Directive()
export abstract class AutocompleteBaseDirective<E extends BaseType, M> implements OnDestroy {

  protected pendingBlur = false;

  protected isFetching = false;

  protected dirty = false;

  private _useFullEntityId = false;

  protected get useFullEntityId() {
    return this._useFullEntityId;
  }

  protected set useFullEntityId(value) {
    this._useFullEntityId = value;
  }

  protected searchText = '';

  protected abstract getControl(): FormControl;

  protected abstract getAutocompleteTrigger(): MatAutocompleteTrigger;

  protected abstract getInput(): ElementRef<HTMLInputElement>;

  protected abstract getFilteredEntities(): Observable<Array<E>>;

  protected abstract getModelValue(): M | null;

  protected abstract updateView(value: M | E, entity: E): void;

  protected abstract isCreateNew(): boolean;

  protected destroy$ = new Subject<void>();

  protected onTouched: () => void = () => {};

  protected getDisplayName(entity: E): string {
    return entity.name ?? '';
  }

  protected reset(): void {
    this.getControl().patchValue('', { emitEvent: false });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  clear(): void {
    this.getControl().patchValue('', { emitEvent: true });
    setTimeout(() => {
      this.getInput().nativeElement.blur();
      this.getInput().nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  onFocus(): void {
    this.pendingBlur = false;
    if (this.dirty) {
      this.getControl().updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  onBlur(): void {
    this.onTouched();
    if (this.isFetching) {
      this.pendingBlur = true;
    } else {
      this.getFilteredEntities().pipe(
        first(),
        takeUntil(this.destroy$)
      ).subscribe(entities => this.performValidation(entities));
    }
  }

  performValidation(entities: E[]): void {
    const searchLower = this.searchText?.trim().toLowerCase() ?? '';
    if (this.getModelValue() || !entities || !searchLower) {
      this.pendingBlur = false;
      return;
    }
    if (entities.length === 1) {
      const entity = entities[0];
      const nameLower = this.getDisplayName(entity)?.toLowerCase() ?? '';
      if (this.isCreateNew() || nameLower.includes(searchLower)) {
        this.selectMatchedEntity(entity);
        return;
      }
    } else {
      const exactMatches = entities.filter(e => this.getDisplayName(e)?.toLowerCase() === searchLower);
      if (exactMatches.length === 1) {
        this.selectMatchedEntity(exactMatches[0]);
        return;
      }
    }

    this.pendingBlur = false;
  }

  protected selectMatchedEntity(entity: E): void {
    this.pendingBlur = false;
    this.searchText = this.getDisplayName(entity);
    this.getControl().patchValue(entity, { emitEvent: false });
    const rawId = entity.id;
    if (rawId == null) {
      return;
    }
    const newModelValue = (this.useFullEntityId ? rawId : typeof rawId === 'string' ? rawId : rawId.id) as M;
    this.updateView(newModelValue, entity);
    this.getAutocompleteTrigger()?.closePanel();
  }
}
