// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  createColorMarkerIconElement,
  MarkerIconContainer, markerIconContainers,
  tripMarkerIconContainers
} from '@shared/models/widget/maps/marker-shape.models';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import tinycolor from 'tinycolor2';
import { map, share } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

export interface MarkerIconInfo {
  iconContainer?: MarkerIconContainer;
  icon: string;
}

interface MarkerIconContainerInfo {
  iconContainer: MarkerIconContainer;
  html$: Observable<SafeHtml>;
}

@Component({
    selector: 'tb-marker-icon-shapes',
    templateUrl: './marker-icon-shapes.component.html',
    providers: [],
    styleUrls: ['./marker-icon-shapes.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MarkerIconShapesComponent extends PageComponent implements OnInit {

  @Input()
  icon: string;

  @Input()
  iconContainer: MarkerIconContainer;

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  trip = false;

  @Input()
  popover: TbPopoverComponent<MarkerIconShapesComponent>;

  @Output()
  markerIconSelected = new EventEmitter<MarkerIconInfo>();

  dirty = false;

  iconContainers: MarkerIconContainerInfo[];

  constructor(protected store: Store<AppState>,
              private iconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer) {
    super(store);
  }

  ngOnInit(): void {
    this.updateIconContainers();
  }

  cancel() {
    this.popover?.hide();
  }

  selectIcon(icon: string) {
    if (this.icon !== icon) {
      this.icon = icon;
      this.dirty = true;
      this.updateIconContainers();
    }
  }

  selectIconContainer(iconContainer: MarkerIconContainer) {
    if (this.iconContainer !== iconContainer) {
      this.iconContainer = iconContainer;
      this.dirty = true;
    }
  }

  apply() {
    const iconInfo: MarkerIconInfo = {
      iconContainer: this.iconContainer,
      icon: this.icon
    };
    this.markerIconSelected.emit(iconInfo);
  }

  private updateIconContainers() {
    const containersList = [...(this.trip ? tripMarkerIconContainers : markerIconContainers),null];
    this.iconContainers = containersList.map((iconContainer) => {
      return {
        iconContainer,
        html$: createColorMarkerIconElement(this.iconRegistry, this.domSanitizer, iconContainer, this.icon, tinycolor(this.color)).pipe(
          map((element) => {
            return this.domSanitizer.bypassSecurityTrustHtml(element.outerHTML);
          }),
          share()
        )
      };
    });
  }
}
