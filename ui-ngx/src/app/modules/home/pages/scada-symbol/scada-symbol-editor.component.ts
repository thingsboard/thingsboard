// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ScadaSymbolEditObject,
  ScadaSymbolEditObjectCallbacks
} from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { FormControl } from '@angular/forms';
import {
  parseScadaSymbolsTagsFromContent,
  removeScadaSymbolMetadata
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface ScadaSymbolEditorData {
  scadaSymbolContent: string;
}

type editorModeType = 'svg' | 'xml';

@Component({
    selector: 'tb-scada-symbol-editor',
    templateUrl: './scada-symbol-editor.component.html',
    styleUrls: ['./scada-symbol-editor.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScadaSymbolEditorComponent implements OnInit, OnDestroy, AfterViewInit, OnChanges {

  @ViewChild('scadaSymbolShape', {static: false})
  scadaSymbolShape: ElementRef<HTMLElement>;

  @ViewChild('tooltipsContainer', {static: false})
  tooltipsContainer: ElementRef<HTMLElement>;

  @ViewChild('tooltipsContainerComponent', {static: true})
  tooltipsContainerComponent: TbAnchorComponent;

  @Input()
  data: ScadaSymbolEditorData;

  @Input()
  editObjectCallbacks: ScadaSymbolEditObjectCallbacks;

  @Input()
  readonly: boolean;

  @Output()
  updateScadaSymbol = new EventEmitter();

  @Output()
  downloadScadaSymbol = new EventEmitter();

  scadaSymbolEditObject: ScadaSymbolEditObject;

  zoomInDisabled = false;
  zoomOutDisabled = false;

  @Input()
  showHiddenElements = false;

  @Output()
  showHiddenElementsChange = new EventEmitter<boolean>();

  displayShowHidden = false;

  svgContentFormControl = new FormControl();

  svgContent: string;

  private editorModeValue: editorModeType = 'svg';

  get editorMode(): editorModeType {
    return this.editorModeValue;
  }

  set editorMode(value: editorModeType) {
    this.updateEditorMode(value);
  }

  constructor(private cd: ChangeDetectorRef,
              private zone: NgZone,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.readonly) {
      this.svgContentFormControl.disable({emitEvent: false});
    } else {
      this.svgContentFormControl.enable({emitEvent: false});
    }
    this.svgContentFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((svgContent) => {
      if (this.svgContent !== svgContent) {
        this.svgContent = svgContent;
        this.editObjectCallbacks.onSymbolEditObjectDirty(true);
      }
      this.editObjectCallbacks.onSymbolEditObjectValid(this.svgContentFormControl.valid);
    });
  }

  ngAfterViewInit() {
    this.editObjectCallbacks.onZoom = () => {
      this.updateZoomButtonsState();
    };
    this.editObjectCallbacks.hasHiddenElements = (hasHidden) => {
      this.displayShowHidden = hasHidden;
      if (hasHidden) {
        this.scadaSymbolEditObject.showHiddenElements(this.showHiddenElements);
      }
      this.cd.markForCheck();
    };
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.scadaSymbolShape.nativeElement,
      this.tooltipsContainer.nativeElement,
      this.tooltipsContainerComponent.viewContainerRef, this.zone, this.editObjectCallbacks, this.readonly);
    if (this.data) {
      this.updateContent(this.data.scadaSymbolContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          setTimeout(() => {
            this.updateContent(this.data.scadaSymbolContent);
          });
        } else if (propName === 'readonly') {
          this.scadaSymbolEditObject.setReadOnly(this.readonly);
          if (this.readonly) {
            this.svgContentFormControl.disable({emitEvent: false});
          } else {
            this.svgContentFormControl.enable({emitEvent: false});
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.scadaSymbolEditObject.destroy();
  }

  getContent(): string {
    if (this.editorMode === 'svg') {
      return this.scadaSymbolEditObject?.getContent();
    } else {
      return this.svgContent;
    }
  }

  getTags(): string[] {
    if (this.editorMode === 'svg') {
      return this.scadaSymbolEditObject?.getTags();
    } else {
      return parseScadaSymbolsTagsFromContent(this.svgContent);
    }
  }

  zoomIn() {
    this.scadaSymbolEditObject.zoomIn();
  }

  zoomOut() {
    this.scadaSymbolEditObject.zoomOut();
  }

  toggleShowHidden() {
    this.showHiddenElements = !this.showHiddenElements;
    this.showHiddenElementsChange.emit(this.showHiddenElements);
    this.scadaSymbolEditObject.showHiddenElements(this.showHiddenElements);
  }

  private updateEditorMode(mode: editorModeType) {
    this.editorModeValue = mode;
    if (mode === 'xml') {
      this.svgContent = this.scadaSymbolEditObject.getContent();
      this.svgContentFormControl.setValue(this.svgContent, {emitEvent: false});
    } else {
      this.updateEditObjectContent(this.svgContent);
    }
  }

  private updateContent(content: string) {
    this.svgContent = removeScadaSymbolMetadata(content);
    if (this.editorMode === 'xml') {
      this.svgContentFormControl.setValue(this.svgContent, {emitEvent: false});
    } else {
      this.updateEditObjectContent(this.svgContent);
    }
  }

  private updateEditObjectContent(content: string) {
    if (this.scadaSymbolEditObject) {
      this.displayShowHidden = false;
      this.scadaSymbolEditObject.setContent(content);
      setTimeout(() => {
        this.updateZoomButtonsState();
      });
    }
  }

  private updateZoomButtonsState() {
    this.zoomInDisabled = this.scadaSymbolEditObject.zoomInDisabled();
    this.zoomOutDisabled = this.scadaSymbolEditObject.zoomOutDisabled();
    this.cd.markForCheck();
  }
}

