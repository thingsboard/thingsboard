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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  createColorMarkerShapeURI,
  MarkerShape, markerShapes,
  tripMarkerShapes
} from '@shared/models/widget/maps/marker-shape.models';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import tinycolor from 'tinycolor2';
import { map, share } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

interface MarkerShapeInfo {
  shape: MarkerShape;
  url$: Observable<SafeUrl>;
}

@Component({
  selector: 'tb-marker-shapes',
  templateUrl: './marker-shapes.component.html',
  providers: [],
  styleUrls: ['./marker-shapes.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MarkerShapesComponent extends PageComponent implements OnInit {

  @Input()
  shape: MarkerShape;

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  trip = false;

  @Input()
  popover: TbPopoverComponent<MarkerShapesComponent>;

  @Output()
  markerShapeSelected = new EventEmitter<MarkerShape>();

  shapes: MarkerShapeInfo[];

  constructor(protected store: Store<AppState>,
              private iconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer) {
    super(store);
  }

  ngOnInit(): void {
    this.shapes = (this.trip ? tripMarkerShapes : markerShapes).map((shape) => {
      return {
        shape,
        url$: createColorMarkerShapeURI(this.iconRegistry, this.domSanitizer, shape, tinycolor(this.color)).pipe(
          map((url) => {
            return this.domSanitizer.bypassSecurityTrustUrl(url);
          }),
          share()
        )
      };
    });
  }

  cancel() {
    this.popover?.hide();
  }

  selectShape(shape: MarkerShape) {
    this.markerShapeSelected.emit(shape);
  }
}
