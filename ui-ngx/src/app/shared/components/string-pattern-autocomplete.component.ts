///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  QueryList,
  SimpleChanges,
  TemplateRef,
  ViewChild,
  ViewChildren,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ConnectedPosition,
  FlexibleConnectedPositionStrategy,
  Overlay,
  OverlayRef,
  PositionStrategy
} from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';

@Component({
  selector: 'tb-string-pattern-autocomplete',
  templateUrl: './string-pattern-autocomplete.component.html',
  styleUrls: ['./string-pattern-autocomplete.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringPatternAutocompleteComponent),
      multi: true
    }
  ]
})
export class StringPatternAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('inputRef', {static: true}) inputRef: ElementRef;
  @ViewChild('highlightTextRef', {static: true}) highlightTextRef: ElementRef;
  @ViewChild('autocompleteTemplate', {static: true}) autocompleteTemplate: TemplateRef<any>;
  @ViewChildren('optionItem', { read: ElementRef }) optionItems!: QueryList<ElementRef<HTMLElement>>;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input({required: true})
  predefinedValues: Array<string>;

  @Input()
  placeholderText: string = this.translate.instant('widget-config.set');

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  additionalClass: string | string[] | Record<string, boolean | undefined | null>;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  label: string;

  @Input()
  tooltipClass = 'tb-error-tooltip';

  @Input()
  errorText: string;

  @Input()
  @coerceBoolean()
  showInlineError = false;

  @Input()
  @coerceBoolean()
  predefinedValuesButton = false;

  @Input()
  patternSymbol = '$';

  @Input()
  brackets: 'curly' | 'square';

  selectionFormControl: FormControl;
  filteredOptions: Array<string>;

  searchText = '';
  highlightedHtml = '';
  activeOptionIndex = -1;

  private modelValue: string | null;
  private overlayRef!: OverlayRef;

  private predefinedValuesButtonMode = false;

  private propagateChange = (_val: any) => {
  };

  constructor(private fb: FormBuilder,
              private overlay: Overlay,
              private translate: TranslateService,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.selectionFormControl = this.fb.control('', this.required ? [Validators.required] : []);
    merge(
      fromEvent(this.inputRef.nativeElement, 'selectionchange'),
      this.selectionFormControl.valueChanges.pipe(tap(value => this.updateView(value)))
    ).pipe(
      debounceTime(50),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.onSelectionChange();
      this.cd.markForCheck();
    });

    fromEvent<KeyboardEvent>(this.inputRef.nativeElement, 'keydown')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => this.handleKeydown(event));
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'predefinedValues' || propName === 'patternSymbol') {
          this.highlightedHtml = this.getHighlightHtml(this.modelValue);
        }
        if (propName === 'required') {
          if (change.currentValue) {
            this.selectionFormControl.addValidators(Validators.required);
          } else {
            this.selectionFormControl.removeValidators(Validators.required);
          }
          this.selectionFormControl.updateValueAndValidity({emitEvent: false})
        }
      }
    }
  }

  writeValue(option?: string): void {
    this.searchText = '';
    this.modelValue = option ?? null;
    this.highlightedHtml = this.getHighlightHtml(this.modelValue);
    this.selectionFormControl.patchValue(this.modelValue, {emitEvent: false});
  }

  onFocus() {
    this.onSelectionChange();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectionFormControl.disable({emitEvent: false});
      this.closeAutocomplete();
    } else {
      this.selectionFormControl.enable({emitEvent: false});
    }
  }

  onInputScroll(event: Event) {
    const scrollLeft = (event.target as HTMLInputElement).scrollLeft;
    const scrollTop = (event.target as HTMLInputElement).scrollTop;
    if (this.highlightTextRef && this.highlightTextRef.nativeElement) {
      this.highlightTextRef.nativeElement.scrollLeft = scrollLeft;
      this.highlightTextRef.nativeElement.scrollTop = scrollTop;
    }
  }

  optionSelected(value: string) {
    const position = this.inputRef.nativeElement.selectionStart;
    const triggerIndex = this.predefinedValuesButtonMode ? position : this.modelValue.lastIndexOf(this.patternSymbol, position - 1);
    if (triggerIndex === -1) {
      return;
    }
    let prepareValue: string;
    switch (this.brackets) {
      case 'curly':
        prepareValue = `{${value}}`;
        break;
      case 'square':
        prepareValue = `[${value}]`;
        break;
      default:
        prepareValue = value;
    }
    const base = this.modelValue ?? '';
    const newText = `${base.substring(0, triggerIndex)}${this.patternSymbol}${prepareValue}${base.substring(position)}`;
    this.selectionFormControl.patchValue(newText);
    this.searchText = '';
    setTimeout(() => {
      this.inputRef.nativeElement.setSelectionRange(triggerIndex + prepareValue.length + 1, triggerIndex + prepareValue.length + 1);
      this.inputRef.nativeElement.focus();
    });
    this.closeAutocomplete();
  }

  openValues($event: MouseEvent) {
    $event.stopPropagation();
    const selectionStart = this.inputRef.nativeElement.selectionStart;
    this.filteredOptions = [...this.predefinedValues];
    this.predefinedValuesButtonMode = true;
    this.openAutocomplete(selectionStart);
  }

  private updateView(value: string) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.highlightedHtml = this.getHighlightHtml(this.modelValue);
      this.propagateChange(this.modelValue);
    }
  }

  private onSelectionChange() {
    const selectionStart = this.inputRef.nativeElement.selectionStart;
    const selectionEnd = this.inputRef.nativeElement.selectionEnd;
    if (selectionStart === selectionEnd && selectionStart > 0 && !this.disabled) {
      this.filteredOptions = this.getFilteredOptions(this.modelValue, selectionStart);
      this.activeOptionIndex = 0;
    } else {
      this.filteredOptions = [];
      this.activeOptionIndex = -1;
    }
    if (this.filteredOptions.length) {
      this.openAutocomplete(selectionStart);
    } else {
      this.closeAutocomplete()
    }
  }

  private getHighlightHtml(text: string): string {
    if (!text) {
      return '';
    }
    let regex: RegExp;
    let simpleGroup = false;
    text = text.replace(/</g, '&lt;').replace(/>/g, '&gt;');
    switch (this.brackets) {
      case 'curly':
        regex = new RegExp(`([${this.patternSymbol}](\\{([^}]*)\\}))`, 'g');
        break;
      case 'square':
        regex = new RegExp(`([${this.patternSymbol}](\\[([^]]*)\\]))`, 'g');
        break;
      default:
        regex = new RegExp(`([${this.patternSymbol}](\\w+))`, 'g');
        simpleGroup = true
        break
    }
    return text.replace(regex, (_match: string, p1: string, p2: string, p3: string) => {
      const value = simpleGroup ? p2 : p3;
      if (this.predefinedValues.includes(value)) {
        return `<span class="highlight">${p1}</span>`
      }
      return p1;
    });
  }

  private getFilteredOptions(text: string, index = text.length): Array<string> {
    const triggerIndex = text.lastIndexOf(this.patternSymbol, index - 1);
    if (triggerIndex === -1) {
      return [];
    }

    let currentWordEndIndex = text.indexOf(' ', index);

    if (currentWordEndIndex === -1) {
      currentWordEndIndex = text.length;
    }

    let startOffset = 1;
    switch (this.brackets) {
      case 'curly':
        if (text[triggerIndex + startOffset] === '{') {
          startOffset++;
        }
        break;
      case 'square':
        if (text[triggerIndex + startOffset] === '[') {
          startOffset++;
        }
        break;
    }

    this.searchText = text.substring(triggerIndex + startOffset, currentWordEndIndex).toLowerCase();
    if (this.searchText.includes(' ')) {
      return [];
    }

    const result = this.predefinedValues.filter(value => value.toLowerCase().startsWith(this.searchText));
    if (result.length === 1 && result[0].toLowerCase() === this.searchText) {
      return [];
    }
    return result;
  }

  private openAutocomplete(cursorIndex?: number): void {
    const patternIndex = this.predefinedValuesButtonMode ? cursorIndex : this.modelValue.lastIndexOf(this.patternSymbol, cursorIndex - 1);
    if (!this.overlayRef) {
      this.overlayRef = this.overlay.create({
        positionStrategy: this.getOverlayPosition(patternIndex),
        scrollStrategy: this.overlay.scrollStrategies.reposition(),
        panelClass: 'tb-select-overlay',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true
      });
      this.overlayRef.backdropClick().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.closeAutocomplete());
    }
    if (!this.overlayRef.hasAttached()) {
      const templatePortal = new TemplatePortal(this.autocompleteTemplate, this.viewContainerRef);
      this.overlayRef.attach(templatePortal);
      this.overlayRef.updatePositionStrategy(this.getOverlayPosition(patternIndex))
    }
  }

  private closeAutocomplete(): void {
    this.predefinedValuesButtonMode = false;
    if (this.overlayRef && this.overlayRef.hasAttached()) {
      this.overlayRef.detach();
    }
  }

  private getOverlayPosition(patternIndex: number): PositionStrategy {
    const strategy = this.overlay
      .position()
      .flexibleConnectedTo(this.inputRef)
      .withFlexibleDimensions(false)
      .withPush(false);

    this.setStrategyPositions(strategy, patternIndex);
    return strategy;
  }

  private setStrategyPositions(positionStrategy: FlexibleConnectedPositionStrategy, patternIndex: number) {
    let offsetX = patternIndex > 0 ? patternIndex * 8 : 0;
    const inputBounds = this.inputRef.nativeElement.getBoundingClientRect();
    if (offsetX + 180 > inputBounds.width) {
      offsetX = inputBounds.width - 180;
    }
    const belowPositions: ConnectedPosition[] = [
      {originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetX},
      {originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetX},
    ];

    const panelClass = 'mat-mdc-autocomplete-panel-above';
    const abovePositions: ConnectedPosition[] = [
      {originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', panelClass, offsetX},
      {originX: 'end', originY: 'top', overlayX: 'end', overlayY: 'bottom', panelClass, offsetX},
    ];

    const positions = [...belowPositions, ...abovePositions];
    positionStrategy.withPositions(positions);
  }

  private setActiveOption(index: number): void {
    if (this.filteredOptions.length === 0) {
      this.activeOptionIndex = -1;
      return;
    }

    this.activeOptionIndex = Math.max(-1, Math.min(index, this.filteredOptions.length - 1));
    this.cd.markForCheck();

    if (this.activeOptionIndex >= 0 && this.optionItems.get(this.activeOptionIndex)) {
      this.optionItems.get(this.activeOptionIndex).nativeElement.scrollIntoView({ block: 'nearest' })
    }
  }

  private handleKeydown(event: KeyboardEvent): void {
    if (!this.overlayRef?.hasAttached()) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.setActiveOption(this.activeOptionIndex + 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.setActiveOption(this.activeOptionIndex - 1);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.activeOptionIndex >= 0 && this.activeOptionIndex < this.filteredOptions.length) {
          this.optionSelected(this.filteredOptions[this.activeOptionIndex]);
        }
        break;
      case 'Escape':
        this.closeAutocomplete();
        break;
    }
  }
}
