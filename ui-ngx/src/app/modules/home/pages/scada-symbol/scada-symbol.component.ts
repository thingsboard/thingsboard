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

import { AfterViewInit, Component, HostBinding, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ScadaSymbolData } from '@home/pages/scada-symbol/scada-symbol.models';
import { IotSvgMetadata, parseIotSvgMetadataFromContent } from '@home/components/widget/lib/svg/iot-svg.models';

@Component({
  selector: 'tb-scada-symbol',
  templateUrl: './scada-symbol.component.html',
  styleUrls: ['./scada-symbol.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  symbolData: ScadaSymbolData;
  metadata: IotSvgMetadata;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute) {
    super(store);
    this.route.data.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.reset();
        this.init();
      }
    );
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private reset(): void {
  }

  private init() {
    this.symbolData = this.route.snapshot.data.symbolData;
    this.metadata = parseIotSvgMetadataFromContent(this.symbolData.svgContent);
  }
}

