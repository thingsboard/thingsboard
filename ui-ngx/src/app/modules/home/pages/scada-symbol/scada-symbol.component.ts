///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  HostBinding,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ScadaSymbolData } from '@home/pages/scada-symbol/scada-symbol.models';
import { IotSvgMetadata, parseIotSvgMetadataFromContent } from '@home/components/widget/lib/svg/iot-svg.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { deepClone } from '@core/utils';
import {
  ScadaSymbolEditorComponent,
  ScadaSymbolEditorData
} from '@home/pages/scada-symbol/scada-symbol-editor.component';

@Component({
  selector: 'tb-scada-symbol',
  templateUrl: './scada-symbol.component.html',
  styleUrls: ['./scada-symbol.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  @ViewChild('symbolEditor')
  symbolEditor: ScadaSymbolEditorComponent;

  symbolData: ScadaSymbolData;
  symbolEditorData: ScadaSymbolEditorData;
  metadata: IotSvgMetadata;

  previewMode = false;

  scadaSymbolFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private origSymbolData: ScadaSymbolData;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.scadaSymbolFormGroup = this.fb.group({
      metadata: [null]
    });
    this.route.data.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.reset();
        this.init(this.route.snapshot.data.symbolData);
      }
    );
  }

  ngAfterViewInit() {
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  onApplyScadaSymbolConfig() {
    if (this.scadaSymbolFormGroup.valid) {
      const svgContent = this.symbolEditor.getContent();
      console.log(svgContent);
    }
  }

  onRevertScadaSymbolConfig() {
    this.init(this.origSymbolData);
  }

  private reset(): void {
    this.previewMode = false;
  }

  private init(data: ScadaSymbolData) {
    this.origSymbolData = data;
    this.symbolData = deepClone(data);
    this.symbolEditorData = {
      svgContent: this.symbolData.svgContent
    };
    this.metadata = parseIotSvgMetadataFromContent(this.symbolData.svgContent);
    this.scadaSymbolFormGroup.patchValue({
      metadata: this.metadata
    }, {emitEvent: false});
    this.scadaSymbolFormGroup.markAsPristine();
    this.cd.markForCheck();
  }
}

