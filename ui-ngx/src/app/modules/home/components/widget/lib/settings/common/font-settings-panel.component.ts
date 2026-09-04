// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  commonFonts,
  ComponentStyle, cssUnit,
  Font,
  fontStyles,
  fontStyleTranslations,
  fontWeights,
  fontWeightTranslations, isFontPartiallySet,
  textStyle
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { map, startWith, tap } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-font-settings-panel',
    templateUrl: './font-settings-panel.component.html',
    providers: [],
    styleUrls: ['./font-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class FontSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  font: Font;

  @Input()
  previewText = 'AaBbCcDd';

  @Input()
  initialPreviewStyle: ComponentStyle;

  @Input()
  @coerceBoolean()
  clearButton = false;

  @Input()
  @coerceBoolean()
  autoScale = false;

  @Input()
  @coerceBoolean()
  disabledLineHeight = false;

  @Input()
  forceSizeUnit: cssUnit;

  @Input()
  popover: TbPopoverComponent<FontSettingsPanelComponent>;

  @Output()
  fontApplied = new EventEmitter<Font>();

  @ViewChild('familyInput', {static: true}) familyInput: ElementRef;

  fontWeightsList = fontWeights;

  fontWeightTranslationsMap = fontWeightTranslations;

  fontStylesList = fontStyles;

  fontStyleTranslationsMap = fontStyleTranslations;

  fontFormGroup: UntypedFormGroup;

  filteredFontFamilies: Observable<Array<string>>;

  familySearchText = '';

  previewStyle: ComponentStyle = {};

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.fontFormGroup = this.fb.group(
      {
        size: [{value: this.font?.size, disabled: this.autoScale}, [Validators.min(0)]],
        sizeUnit: [{ value: (!!this.forceSizeUnit ?
            this.forceSizeUnit : (this.font?.sizeUnit || 'px')), disabled: this.autoScale || !!this.forceSizeUnit}, []],
        family: [this.font?.family, []],
        weight: [this.font?.weight, []],
        style: [this.font?.style, []],
        lineHeight: [{ value: this.font?.lineHeight, disabled: this.autoScale || this.disabledLineHeight }, []]
      }
    );
    this.updatePreviewStyle(this.font);
    this.fontFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((font: Font) => {
      if (this.fontFormGroup.valid) {
        this.updatePreviewStyle(font);
        setTimeout(() => {this.popover?.updatePosition();}, 0);
      }
    });
    this.filteredFontFamilies = this.fontFormGroup.get('family').valueChanges
      .pipe(
        startWith<string>(''),
        tap((searchText) => { this.familySearchText = searchText || ''; }),
        map(() => commonFonts.filter(f => f.toUpperCase().includes(this.familySearchText.toUpperCase())))
      );
  }

  clearFamily() {
    this.fontFormGroup.get('family').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.familyInput.nativeElement.blur();
      this.familyInput.nativeElement.focus();
    }, 0);
  }

  cancel() {
    this.popover?.hide();
  }

  applyFont() {
    const font = this.fontFormGroup.value;
    this.fontApplied.emit(font);
  }

  clearDisabled(): boolean {
    return !isFontPartiallySet(this.fontFormGroup.value);
  }

  clearFont() {
    this.fontFormGroup.reset({sizeUnit: this.forceSizeUnit || 'px'});
    this.fontFormGroup.markAsDirty();
  }

  private updatePreviewStyle(font: Font) {
    if (!!this.forceSizeUnit) {
      font = {...font, ...{sizeUnit: this.forceSizeUnit}};
    }
    this.previewStyle = {...(this.initialPreviewStyle || {}), ...textStyle(font)};
  }

}
