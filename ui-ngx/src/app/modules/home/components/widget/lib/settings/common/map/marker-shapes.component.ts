// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    encapsulation: ViewEncapsulation.None,
    standalone: false
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
