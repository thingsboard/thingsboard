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

import L, { TB } from 'leaflet';
import { guid, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import 'leaflet-providers';
import { Map as MapLibreGLMap, LngLat as MapLibreGLLngLat } from 'maplibre-gl';
import '@geoman-io/leaflet-geoman-free';
import 'leaflet.markercluster';
import { MatIconRegistry } from '@angular/material/icon';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { catchError, take } from 'rxjs/operators';
import { of } from 'rxjs';

L.MarkerCluster = L.MarkerCluster.mergeOptions({ pmIgnore: true });

L.Map.addInitHook(function () {
  this._patterns = {};
});

L.Map.include({

  addPattern: function (pattern: Pattern) {
    const id = L.stamp(pattern);
    if (this._patterns[id]) {
      return pattern;
    }
    this._patterns[id] = pattern;
    this.whenReady(() => {
      pattern.onAdd(this);
    });
    return this;
  },

  removePattern: function (pattern: Pattern) {
    const id = L.stamp(pattern);
    if (!this._patterns[id]) {
      return this;
    }
    if (this._loaded) {
      pattern.onRemove(this);
    }
    delete this._patterns[id];

    if (this._loaded) {
      this.fire('patternremove', {pattern: pattern});
      pattern.fire('remove');
    }

    pattern._map = null;
    return this;
  },

  _initDefRoot: function () {
    if (!this._defRoot) {
      const renderer: L.Renderer = this.getRenderer(this);
      this._defRoot = Pattern.prototype._createElement('defs');
      ((renderer as any)._container).appendChild(this._defRoot);
    }
  }
});

L.SVG.include({
  _superUpdateStyle: (L.SVG.prototype as any)._updateStyle,

  _updateStyle: function (layer: L.Layer){
    this._superUpdateStyle(layer);
    const options: L.PathOptions = layer.options;
    if (options.fill && options.fillPattern) {
      ((layer as any)._path as SVGElement).setAttribute('fill', 'url(#' + L.stamp(options.fillPattern) + ")");
    }
  }
})

class SidebarControl extends L.Control<TB.SidebarControlOptions> implements L.TB.SidebarControl {

  private readonly sidebar: JQuery<HTMLElement>;

  private current = $();
  private currentButton = $();

  private map: L.Map;

  private buttonContainer: JQuery<HTMLElement>;

  constructor(options: TB.SidebarControlOptions) {
    super(options);
    this.sidebar = $('<div class="tb-map-sidebar"></div>');
    this.options.container.append(this.sidebar);
    const position = options?.position || 'topleft';
    if (['topleft', 'bottomleft'].includes(position)) {
      this.options.container.addClass('tb-sidebar-left');
    } else {
      this.options.container.addClass('tb-sidebar-right');
    }
  }

  addPane(pane: JQuery<HTMLElement>, button: JQuery<HTMLElement>): this {
    pane.hide().appendTo(this.sidebar);
    button.appendTo(this.buttonContainer);
    return this;
  }

  togglePane(pane: JQuery<HTMLElement>, button: JQuery<HTMLElement>) {
    const paneWidth = this.options?.paneWidth || 220;
    const position = this.options?.position || 'topleft';

    this.current.hide().trigger('hide');
    this.currentButton.removeClass('active');


    if (this.current === pane) {
      if (['topleft', 'bottomleft'].includes(position)) {
        this.map.panBy([-paneWidth, 0], { animate: false });
      }
      this.sidebar.hide();
      this.current = this.currentButton = $();
    } else {
      this.sidebar.show();
      if (['topleft', 'bottomleft'].includes(position) && !this.current.length) {
        this.map.panBy([paneWidth, 0], { animate: false });
      }
      this.current = pane;
      this.currentButton = button || $();
    }
    this.current.show().trigger('show');
    this.currentButton.addClass('active');
    this.map.invalidateSize({ pan: false, animate: false });
  }

  onAdd(map: L.Map): HTMLElement {
    this.buttonContainer = $("<div>")
    .attr('class', 'leaflet-bar');
    return this.buttonContainer[0];
  }

  addTo(map: L.Map): this {
    this.map = map;
    return super.addTo(map);
  }
}

class SidebarPaneControl<O extends TB.SidebarPaneControlOptions> extends L.Control<O> implements L.TB.SidebarPaneControl<O> {

  private button: JQuery<HTMLElement>;
  private $ui: JQuery<HTMLElement>;

  constructor(options: O) {
    super(options);
  }

  addTo(map: L.Map): this {

    this.button = $("<a>")
    .attr('class', 'tb-control-button')
    .attr('href', '#')
    .attr('role', 'button')
    .html('<div class="' + this.options.uiClass + '"></div>')
    .on('click', (e) => {
      this.toggle(e);
    });
    if (this.options.buttonTitle) {
      this.button.attr('title', this.options.buttonTitle);
    }

    this.$ui = $('<div>')
        .attr('class', this.options.uiClass);

    $('<div class="tb-layers-title-container">')
    .appendTo(this.$ui)
    .append($('<div class="tb-layers-title">')
    .text(this.options.paneTitle))
    .append($('<div>')
    .append($('<button type="button" class="tb-button-close mdc-icon-button mat-mdc-icon-button">' +
      '<span class="mat-mdc-button-persistent-ripple mdc-icon-button__ripple"></span>' +
      '<span class="material-icons">close</span>' +
      '</button>')
    .attr('aria-label', 'Close')
    .bind('click', (e) => {
      this.toggle(e);
    })));

    this.options.sidebar.addPane(this.$ui, this.button);

    this.onAddPane(map, this.button, this.$ui, (e) => {
      this.toggle(e);
    });

    return this;
  }

  public onAddPane(map: L.Map, button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, toggle: (e: JQuery.MouseEventBase) => void) {}

  private toggle(e: JQuery.MouseEventBase) {
    e.stopPropagation();
    e.preventDefault();
    if (!this.button.hasClass("disabled")) {
      this.options.sidebar.togglePane(this.$ui, this.button);
    }
  }
}

class LayersControl extends SidebarPaneControl<TB.LayersControlOptions> implements L.TB.LayersControl {
  constructor(options: TB.LayersControlOptions) {
    super(options);
  }

  public onAddPane(map: L.Map, button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, toggle: (e: JQuery.MouseEventBase) => void) {
    const paneId = guid();
    const layers = this.options.layers;
    const baseSection = $("<div>")
    .attr('class', 'tb-layers-container')
    .appendTo($ui);

    layers.forEach((layerData, i) => {
      const id = `map-ui-layer-${paneId}-${i}`;
      const buttonContainer = $('<div class="tb-layer-card">')
      .appendTo(baseSection);
      const mapContainer = $('<div class="tb-layer-map">')
      .appendTo(buttonContainer);
      const input = $('<input type="radio" class="tb-layer-button" name="layer">')
      .prop('id', id)
      .prop('checked', map.hasLayer(layerData.layer))
      .appendTo(buttonContainer);

      const item = $('<label class="tb-layer-label">')
      .prop('for', id)
      .append($('<span>').append(layerData.title))
      .appendTo(buttonContainer);

      map.whenReady(() => {

        const miniMap = L.map(mapContainer[0], { attributionControl: false, zoomControl: false, keyboard: false })
        .addLayer(layerData.mini);

        miniMap.dragging.disable();
        miniMap.touchZoom.disable();
        miniMap.doubleClickZoom.disable();
        miniMap.scrollWheelZoom.disable();

        const moved = () => {
          miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 2, 0));
        };

        const shown = () => {
          miniMap.invalidateSize();
          miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 2, 0), { animate: false });
          map.on('moveend', moved);
        };

        $ui.on('show', shown);
        $ui.on('hide', () => {
          map.off('moveend', moved);
        })
      });


      input.on('click', (e: JQuery.MouseEventBase) => {
        e.stopPropagation();
        if (!map.hasLayer(layerData.layer)) {
          map.addLayer(layerData.layer);
          map.attributionControl.setPrefix(layerData.attributionPrefix);
          layers.forEach((other) => {
            if (other.layer !== layerData.layer) {
              map.removeLayer(other.layer);
            }
          });
          map.fire('baselayerchange', { layer: layerData.layer });
        }
      });

      item.on('dblclick', (e) => {
        toggle(e);
      });

      map.on('layeradd layerremove', function () {
        input.prop('checked', map.hasLayer(layerData.layer));
      });
    });
  }
}

class GroupsControl extends SidebarPaneControl<TB.GroupsControlOptions> implements L.TB.GroupsControl {
  constructor(options: TB.GroupsControlOptions) {
    super(options);
  }

  public onAddPane(map: L.Map, _button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, _toggle: (e: JQuery.MouseEventBase) => void) {
    const paneId = guid();
    const groups = this.options.groups;
    const baseSection = $("<div>")
    .attr('class', 'tb-layers-container')
    .appendTo($ui);

    groups.forEach((groupData, i) => {
      const id = `map-group-layer-${paneId}-${i}`;
      const checkBoxContainer = $('<div class="tb-group-checkbox">')
      .appendTo(baseSection);
      const input = $('<input type="checkbox" class="tb-group-button" name="group">')
      .prop('id', id)
      .prop('checked', groupData.enabled)
      .appendTo(checkBoxContainer);

      $('<label class="tb-group-label">')
      .prop('title', groupData.title)
      .prop('for', id)
      .append($('<span>').append(groupData.title))
      .appendTo(checkBoxContainer);
      input.on('click', (e: JQuery.MouseEventBase) => {
        e.stopPropagation();
        groupData.enabled = !groupData.enabled;
        let changed = false;
        groupData.dataLayers.forEach(
          (dl) => {
            changed = dl.toggleGroup(groupData.group) || changed;
          }
        );
        if (changed) {
          map.fire('layergroupchange', {group: groupData});
        }
      });

      map.on('layeradd layerremove', function () {
        input.prop('checked', groupData.enabled);
      });
    });
  }
}

class TopToolbarButton implements L.TB.TopToolbarButton {
  private readonly button: JQuery<HTMLElement>;
  private active = false;
  private disabled = false;
  private _onClick: (e: MouseEvent, button: TopToolbarButton) => void;

  constructor(private readonly options: TB.TopToolbarButtonOptions,
              private readonly iconRegistry: MatIconRegistry) {
    const iconElement = $('<div class="tb-control-button-icon"></div>');
    const setIcon = isNotEmptyStr(this.options.icon);
    const setTitle = isNotEmptyStr(this.options.title);
    this.button = $("<a>")
    .attr('class', 'tb-control-button tb-control-text-button')
    .attr('href', '#')
    .attr('role', 'button');
    if (setIcon) {
      this.button.append(iconElement);
      this.loadIcon(iconElement);
    }
    if (setTitle) {
      this.button.append(`<div class="tb-control-text">${this.options.title}</div>`);
    }
    this.button.css('--tb-map-control-color', this.options.color);
    this.button.css('--tb-map-control-active-color', this.options.color);
    this.button.css('--tb-map-control-hover-background-color', this.options.color);
    if (setIcon && !setTitle) {
      this.button.css('padding', 0);
    } else if (!setIcon && setTitle) {
      this.button.css('padding-left', '14px');
    }
    this.button.on('click', (e) => {
      e.stopPropagation();
      e.preventDefault();
      if (this._onClick) {
        this._onClick(e.originalEvent, this);
      }
    });
  }

  onClick(onClick: (e: MouseEvent, button: TopToolbarButton) => void): void {
   this._onClick = onClick;
  }

  private loadIcon(iconElement: JQuery<HTMLElement>) {
    iconElement.addClass(this.iconRegistry.getDefaultFontSetClass());

    if (!isSvgIcon(this.options.icon)) {
      iconElement.addClass('material-icon-font');
      iconElement.text(this.options.icon);
      return;
    }

    const [namespace, iconName] = splitIconName(this.options.icon);
    this.iconRegistry
      .getNamedSvgIcon(iconName, namespace)
      .pipe(
        take(1),
        catchError((err: Error) => {
          console.log(`Error retrieving icon ${namespace}:${iconName}! ${err.message}`)
          return of(null);
        })
      )
      .subscribe({
        next: (svg) => {
          iconElement.append(svg);
          svg.style.height = '24px';
          svg.style.width = '24px';
        }
      });
  }

  setActive(active: boolean): void {
    if (this.active !== active) {
      this.active = active;
      if (this.active) {
        L.DomUtil.addClass(this.button[0], 'active');
      } else {
        L.DomUtil.removeClass(this.button[0], 'active');
      }
    }
  }

  isActive(): boolean {
    return this.active;
  }

  setDisabled(disabled: boolean): void {
    if (this.disabled !== disabled) {
      this.disabled = disabled;
      if (this.disabled) {
        L.DomUtil.addClass(this.button[0], 'leaflet-disabled');
        this.button[0].setAttribute('aria-disabled', 'true');
      } else {
        L.DomUtil.removeClass(this.button[0], 'leaflet-disabled');
        this.button[0].setAttribute('aria-disabled', 'false');
      }
    }
  }

  isDisabled(): boolean {
    return this.disabled;
  }

  getButtonElement(): JQuery<HTMLElement> {
    return this.button;
  }
}

class ToolbarButton implements L.TB.ToolbarButton {
  private readonly id: string;
  private readonly button: JQuery<HTMLElement>;
  private active = false;
  private disabled = false;

  constructor(private readonly options: TB.ToolbarButtonOptions) {
    this.id = options.id;
    const buttonText = this.options.showText ? this.options.title : null;
    this.button = $("<a>")
    .attr('class', 'tb-control-button')
    .attr('href', '#')
    .attr('role', 'button')
    .html('<div class="'+this.options.iconClass+'"></div>' + (buttonText ? `<div class="tb-control-text">${buttonText}</div>` : ''));
    if (this.options.showText) {
      L.DomUtil.addClass(this.button[0], 'tb-control-text-button');
    } else {
      this.button.attr('title', this.options.title);
    }

    this.button.on('click', (e) => {
      e.stopPropagation();
      e.preventDefault();
      this.options.click(e.originalEvent, this);
    });
  }

  setActive(active: boolean): void {
    if (this.active !== active) {
      this.active = active;
      if (this.active) {
        L.DomUtil.addClass(this.button[0], 'active');
      } else {
        L.DomUtil.removeClass(this.button[0], 'active');
      }
    }
  }

  isActive(): boolean {
    return this.active;
  }

  setDisabled(disabled: boolean): void {
    if (this.disabled !== disabled) {
      this.disabled = disabled;
      if (this.disabled) {
        L.DomUtil.addClass(this.button[0], 'leaflet-disabled');
        this.button[0].setAttribute('aria-disabled', 'true');
      } else {
        L.DomUtil.removeClass(this.button[0], 'leaflet-disabled');
        this.button[0].setAttribute('aria-disabled', 'false');
      }
    }
  }

  isDisabled(): boolean {
    return this.disabled;
  }

  getId(): string {
    return this.id;
  }

  getButtonElement(): JQuery<HTMLElement> {
    return this.button;
  }
}

class TopToolbarControl implements L.TB.TopToolbarControl {

  private readonly toolbarElement: JQuery<HTMLElement>;
  private buttons: Array<TopToolbarButton> = [];

  constructor(private readonly options: TB.TopToolbarControlOptions) {
    const controlContainer = $('.leaflet-control-container', this.options.mapElement);
    this.toolbarElement = $('<div class="tb-map-top-toolbar leaflet-top"></div>');
    this.toolbarElement.appendTo(controlContainer);
  }

  toolbarButton(options: TB.TopToolbarButtonOptions): TopToolbarButton {
    const button = new TopToolbarButton(options, this.options.iconRegistry);
    const buttonContainer = $('<div class="leaflet-bar leaflet-control"></div>');
    button.getButtonElement().appendTo(buttonContainer);
    buttonContainer.appendTo(this.toolbarElement);
    this.buttons.push(button);
    return button;
  }

  setDisabled(disabled: boolean): void {
    this.buttons.forEach(button => {
      if (!button.isActive()) {
        button.setDisabled(disabled);
      }
    });
  }
}

class ToolbarControl extends L.Control<L.ControlOptions> implements L.TB.ToolbarControl {

  private buttonContainer: JQuery<HTMLElement>;

  constructor(options: L.ControlOptions) {
    super(options);
  }

  toolbarButton(options: TB.ToolbarButtonOptions): ToolbarButton {
    const button = new ToolbarButton(options);
    button.getButtonElement().appendTo(this.buttonContainer);
    return button;
  }

  onAdd(map: L.Map): HTMLElement {
    this.buttonContainer = $("<div>")
    .attr('class', 'leaflet-bar');
    return this.buttonContainer[0];
  }

  addTo(map: L.Map): this {
    return super.addTo(map);
  }

}

class BottomToolbarControl implements L.TB.BottomToolbarControl {

  private readonly buttonContainer: JQuery<HTMLElement>;
  private toolbarButtons: ToolbarButton[] = [];

  container: HTMLElement;

  constructor(private readonly options: TB.BottomToolbarControlOptions) {
    const controlContainer = $('.leaflet-control-container', options.mapElement);
    const toolbar = $('<div class="tb-map-bottom-toolbar leaflet-bottom"></div>');
    toolbar.appendTo(controlContainer);
    this.buttonContainer = $('<div class="leaflet-bar leaflet-control"></div>');
    this.buttonContainer.appendTo(toolbar);
    this.container = this.buttonContainer[0];
  }

  getButton(id: string): ToolbarButton {
    return this.toolbarButtons.find(b => b.getId() === id);
  }

  open(buttons: TB.ToolbarButtonOptions[], showCloseButton = true): void {

    this.toolbarButtons.length = 0;

    buttons.forEach(buttonOption => {
      const button = new ToolbarButton(buttonOption);
      this.toolbarButtons.push(button);
      button.getButtonElement().appendTo(this.container);
    });

    if (showCloseButton) {
      const closeButton = $("<a>")
      .attr('class', 'tb-control-button')
      .attr('href', '#')
      .attr('role', 'button')
      .attr('title', this.options.closeTitle)
      .html('<div class="tb-close"></div>');

      closeButton.on('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        this.close();
      });
      closeButton.appendTo(this.buttonContainer);
    }
  }

  close(): void {
    if (this.options.onClose) {
      if (this.options.onClose()) {
        this.buttonContainer.empty();
      }
    } else {
      this.buttonContainer.empty();
    }
  }

}

class Pattern extends L.Evented implements L.TB.Pattern {

  _map: L.Map;
  _dom: SVGPatternElement & HTMLElement;

  private options: L.TB.PatternOptions = {
    x: 0,
    y: 0,
    width: 8,
    height: 8,
    patternUnits: 'userSpaceOnUse',
    patternContentUnits: 'userSpaceOnUse'
  };
  private _elements: {[id: string]: PatternElement} = {};

  constructor(options: L.TB.PatternOptions) {
    super();
    this.options = {...this.options, ...options};
  }

  onAdd(map: L.Map): void {
    this._map = map;
    this._map._initDefRoot();

    this._initDom();

    for (const i in this._elements) {
      this._elements[i].onAdd(this);
    }

    this._addElements();
    this._addDom();
    this.redraw();
    this.fire('add');
    this._map.fire('patternadd', {pattern: this});
  }

  onRemove(_map: L.Map): void {
    this._removeDom();
  }

  redraw(): this {
    if (this._map) {
      this._update();
      for (const i in this._elements) {
        this._elements[i].redraw();
      }
    }
    return this;
  }

  setStyle(style: L.TB.PatternOptions): this {
    L.setOptions(this, style);
    if (this._map) {
      this._updateStyle();
      this.redraw();
    }
    return this;
  }

  addTo(map: L.Map): this {
    map.addPattern(this);
    return this;
  }

  remove(): this {
    return this.removeFrom(this._map);
  }

  removeFrom(map: L.Map): this {
    if (map) {
      map.removePattern(this);
    }
    return this;
  }

  addElement(element: PatternElement): PatternElement | undefined {
    const id = L.stamp(element);
    if (this._elements[id]) {
      return element;
    }
    this._elements[id] = element;
    element.onAdd(this);
  }

  _createElement<E extends SVGElement> (name: string): E {
    return document.createElementNS("http://www.w3.org/2000/svg", name) as E;
  }

  _initDom(): void {
    this._dom = this._createElement('pattern');
    if (this.options.className) {
      L.DomUtil.addClass(this._dom, this.options.className);
    }
    this._updateStyle();
  }

  _addDom(): void {
    this._map._defRoot.appendChild(this._dom);
  }

  _removeDom(): void {
    L.DomUtil.remove(this._dom);
  }

  _updateStyle(): void {
    const dom = this._dom;
    const options = this.options;
    if (!dom) { return; }
    dom.setAttribute('id', `${L.stamp(this)}`);
    dom.setAttribute('x', `${options.x}`);
    dom.setAttribute('y', `${options.y}`);
    dom.setAttribute('width', `${options.width}`);
    dom.setAttribute('height', `${options.height}`);
    dom.setAttribute('patternUnits', options.patternUnits);
    dom.setAttribute('patternContentUnits', options.patternContentUnits);

    if (options.patternTransform || options.angle) {
      let transform = options.patternTransform ? options.patternTransform + " " : "";
      transform += options.angle ?  "rotate(" + options.angle + ") " : "";
      dom.setAttribute('patternTransform', transform);
    } else {
      dom.removeAttribute('patternTransform');
    }
    if (options.viewBox) {
      dom.setAttribute('viewBox', options.viewBox.join(' '));
    } else {
      dom.removeAttribute('viewBox');
    }
    if (options.preserveAspectRatioAlign) {
      let preserveAspectRatioValue = options.preserveAspectRatioAlign;
      if (preserveAspectRatioValue !== 'none' && options.preserveAspectRatioMeetOrSlice) {
        preserveAspectRatioValue += (' ' + options.preserveAspectRatioMeetOrSlice);
      }
      dom.setAttribute('preserveAspectRatio', preserveAspectRatioValue);
    } else {
      dom.removeAttribute('preserveAspectRatio');
    }

    for (const i in this._elements) {
      this._elements[i]._updateStyle();
    }
  }

  protected _addElements() {};
  protected _update() {};

}

abstract class PatternElement<O extends L.TB.PatternElementOptions = L.TB.PatternElementOptions> extends L.Class implements L.TB.PatternElement {

  protected options: O;

  protected _pattern: Pattern;
  protected _dom: SVGElement & HTMLElement;

  protected constructor(options: L.TB.PatternElementOptions) {
    super();
    this.options = {...this._defaultOptions(), ...options};
  }

  onAdd(pattern: Pattern): void {
    this._pattern = pattern;
    if (this._pattern._dom) {
      this._initDom();
      this._addDom();
    }
  }

  addTo(pattern: Pattern): this {
    pattern.addElement(this);
    return this;
  }

  redraw(): this {
    if (this._pattern) {
      this._updateElement();
    }
    return this;
  }

  setStyle(style: L.TB.PatternElementOptions): this {
    L.setOptions(this, style);
    if (this._pattern) {
      this._updateStyle();
    }
    return this;
  }

  _createElement<E extends SVGElement> (name: string): E {
    return document.createElementNS("http://www.w3.org/2000/svg", name) as E;
  }

  _initDomElement(type: string): void {
    this._dom = this._createElement(type);
    if (this.options.className) {
      L.DomUtil.addClass(this._dom, this.options.className);
    }
    this._updateStyle();
  }

  _addDom(): void {
    this._pattern._dom.appendChild(this._dom);
  }

  _updateStyle(): void {}

  protected _initDom() {}
  protected _updateElement() {}

  protected abstract _defaultOptions(): O;
}

const defaultPatternShapeOptions: L.TB.PatternShapeOptions = {
  stroke: true,
  color: '#3388ff',
  weight: 3,
  opacity: 1,
  lineCap: 'round',
  lineJoin: 'round',
  fillOpacity: 0.2,
  fillRule: 'evenodd'
};


abstract class PatternShape<O extends L.TB.PatternShapeOptions> extends PatternElement<O> implements L.TB.PatternShape {

  protected constructor(options: O) {
    super(options);
  }

  _updateStyle(): void {
    const dom = this._dom;
    const options = this.options;
    if (!dom) { return; }
    if (options.stroke) {
      dom.setAttribute('stroke', options.color);
      dom.setAttribute('stroke-opacity', `${options.opacity}`);
      dom.setAttribute('stroke-width', `${options.weight}`);
      dom.setAttribute('stroke-linecap', options.lineCap);
      dom.setAttribute('stroke-linejoin', options.lineJoin);

      if (options.dashArray) {
        dom.setAttribute('stroke-dasharray', options.dashArray.join(' '));
      } else {
        dom.removeAttribute('stroke-dasharray');
      }

      if (options.dashOffset) {
        dom.setAttribute('stroke-dashoffset', `${options.dashOffset}`);
      } else {
        dom.removeAttribute('stroke-dashoffset');
      }
    } else {
      dom.setAttribute('stroke', 'none');
    }

    if (options.fill) {
      if (options.fillPattern) {
        dom.setAttribute('fill', 'url(#' + L.stamp(options.fillPattern) + ")");
      }
      else {
        dom.setAttribute('fill', options.fillColor || options.color);
      }
      dom.setAttribute('fill-opacity', `${options.fillOpacity}`);
      dom.setAttribute('fill-rule', options.fillRule || 'evenodd');
    } else {
      dom.setAttribute('fill', 'none');
    }
    dom.setAttribute('pointer-events', options.pointerEvents || (options.interactive ? 'visiblePainted' : 'none'));
  }
}

class PatternRect extends PatternShape<L.TB.PatternRectOptions> implements L.TB.PatternRect {

  constructor(options: L.TB.PatternRectOptions) {
    super(options);
  }

  protected _initDom() {
    this._initDomElement('rect');
  }

  protected _updateElement() {
    if (!this._dom) { return; }
    this._dom.setAttribute('x', `${this.options.x}`);
    this._dom.setAttribute('y', `${this.options.y}`);
    this._dom.setAttribute('width', `${this.options.width}`);
    this._dom.setAttribute('height', `${this.options.height}`);
    if (this.options.rx) { this._dom.setAttribute('rx', `${this.options.rx}`); }
    if (this.options.ry) { this._dom.setAttribute('ry', `${this.options.ry}`); }
  }

  protected _defaultOptions(): L.TB.PatternRectOptions {
    return {
      x: 0,
      y: 0,
      width: 10,
      height: 10,
      ...defaultPatternShapeOptions
    };
  }
}

class PatternPath extends PatternShape<L.TB.PatternPathOptions> implements L.TB.PatternPath {

  constructor(options: L.TB.PatternPathOptions) {
    super(options);
  }

  protected _initDom() {
    this._initDomElement('path');
  }

  protected _updateElement() {
    if (!this._dom) { return; }
    this._dom.setAttribute('d', this.options.d);
  }

  protected _defaultOptions(): L.TB.PatternPathOptions {
    return {...defaultPatternShapeOptions};
  }
}

class PatternImage extends PatternElement<L.TB.PatternImageOptions> implements L.TB.PatternImage {

  constructor(options: TB.PatternImageOptions) {
    super(options);
  }

  protected _initDom() {
    this._initDomElement('image');
  }

  _updateStyle(): void {
    const dom = this._dom;
    const options = this.options;
    if (!dom) { return; }
    this._dom.setAttribute('href', options.imageUrl);
    this._dom.setAttribute('opacity', isDefinedAndNotNull(options.opacity) ? `${options.opacity}` : '1');
    this._dom.setAttribute('x', '0');
    this._dom.setAttribute('y', '0');
    this._dom.setAttribute('width', `${options.width}`);
    this._dom.setAttribute('height', `${options.height}`);
    if (options.preserveAspectRatioAlign) {
      let preserveAspectRatioValue = options.preserveAspectRatioAlign;
      if (preserveAspectRatioValue !== 'none' && options.preserveAspectRatioMeetOrSlice) {
        preserveAspectRatioValue += (' ' + options.preserveAspectRatioMeetOrSlice);
      }
      dom.setAttribute('preserveAspectRatio', preserveAspectRatioValue);
    } else {
      dom.removeAttribute('preserveAspectRatio');
    }
    const transforms: string[] = [];
    if (options.angle) {
      transforms.push(`rotate(${options.angle})`);
    }
    if (options.scale && options.scale !== 1) {
      transforms.push(`scale(${options.scale})`);
    }
    if (transforms.length) {
      this._dom.setAttribute('transform', transforms.join(' '));
      this._dom.setAttribute('transform-origin', `${options.width/2} ${options.height/2}`);
    }
  }

  protected _defaultOptions(): L.TB.PatternImageOptions {
    return {
      imageUrl: '',
      width: 0,
      height: 0
    };
  }
}

const sidebar = (options: TB.SidebarControlOptions): L.TB.SidebarControl => {
  return new SidebarControl(options);
}

const sidebarPane = <O extends TB.SidebarPaneControlOptions>(options: O): L.TB.SidebarPaneControl<O> => {
  return new SidebarPaneControl(options);
}

const layers = (options: TB.LayersControlOptions): L.TB.LayersControl => {
  return new LayersControl(options);
}

const groups = (options: TB.GroupsControlOptions): L.TB.GroupsControl => {
  return new GroupsControl(options);
}

const topToolbar = (options: TB.TopToolbarControlOptions): L.TB.TopToolbarControl => {
  return new TopToolbarControl(options);
}

const toolbar = (options: L.ControlOptions): L.TB.ToolbarControl => {
  return new ToolbarControl(options);
}

const bottomToolbar = (options: TB.BottomToolbarControlOptions): L.TB.BottomToolbarControl => {
  return new BottomToolbarControl(options);
}

class ChinaProvider extends L.TileLayer implements L.TB.TileLayer.ChinaProvider {

  static chinaProviders: L.TB.TileLayer.ChinaProvidersData = {
    Tencent: {
      Normal: "//rt{s}.map.gtimg.com/tile?z={z}&x={x}&y={-y}&type=vector&styleid=3",
      Satellite: "//p{s}.map.gtimg.com/sateTiles/{z}/{sx}/{sy}/{x}_{-y}.jpg",
      Terrain: "//p{s}.map.gtimg.com/demTiles/{z}/{sx}/{sy}/{x}_{-y}.jpg",
      Subdomains: '0123',
    }
  };

  constructor(type: string, options?: L.TileLayerOptions) {
    options = options || {};

    const parts = type.split('.');
    const providerName = parts[0];
    const mapName = parts[1];

    const url = ChinaProvider.chinaProviders[providerName][mapName];
    options.subdomains = ChinaProvider.chinaProviders[providerName].Subdomains;

    super(url, options);
  }

  getTileUrl(coords: L.Coords): string {
    const data = {
      s: this._getSubdomain(coords),
      x: coords.x,
      y: coords.y,
      z: this._getZoomForUrl(),
      sx: null,
      sy: null
    };
    if (this._map && !this._map.options.crs.infinite) {
      const invertedY = this._globalTileRange.max.y - coords.y;
      if (this.options.tms) {
        data['y'] = invertedY;
      }
      data['-y'] = invertedY;
    }
    data.sx = data.x >> 4;
    data.sy = (( 1 << data.z) - data.y) >> 4;
    return L.Util.template(this._url, L.Util.extend(data, this.options));
  }
}

const chinaProvider = (type: string, options?: L.TileLayerOptions): L.TB.TileLayer.ChinaProvider => {
  return new ChinaProvider(type, options);
}

class MapLibreGLLayer extends L.Layer implements TB.MapLibreGL.MapLibreGLLayer {

  options: TB.MapLibreGL.LeafletMapLibreGLMapOptions;

  private readonly _throttledUpdate: () => void;
  private _container: HTMLDivElement;
  private _glMap: MapLibreGLMap;
  private _actualCanvas: HTMLCanvasElement;
  private _offset: L.Point;
  private _zooming: boolean;

  constructor(options: TB.MapLibreGL.LeafletMapLibreGLMapOptions) {
    super();
    options = {...options, ...{
        updateInterval: 32,
        padding: 0.1,
        interactive: false,
        pane: 'tilePane'
    }};
    options.attribution = this._loadAttribution(options);
    this._prepareTransformRequest(options);
    L.setOptions(this, options);
    this._throttledUpdate = L.Util.throttle(this._update, this.options.updateInterval, this);
  }

  onAdd(map: L.Map): this {
    let update = false;
    if (!this._container) {
      this._initContainer();
    } else {
      update = true;
    }
    const paneName = this.getPaneName();
    map.getPane(paneName).appendChild(this._container);
    this._initGL();

    this._offset = this._map.containerPointToLayerPoint([0, 0]);
    if ((this._map as any)._proxy && map.options.zoomAnimation) {
      L.DomEvent.on((map as any)._proxy, L.DomUtil.TRANSITION_END, this._transitionEnd, this);
    }
    if (update) {
      this._update();
    }
    return this;
  }

  onRemove(map: L.Map): this {
    if ((this._map as any)._proxy && this._map.options.zoomAnimation) {
      L.DomEvent.off((map as any)._proxy, L.DomUtil.TRANSITION_END, this._transitionEnd, this);
    }
    const paneName = this.getPaneName();
    map.getPane(paneName).removeChild(this._container);

    this._glMap.remove();
    this._glMap = null;

    return this;
  }

  getEvents(): { [p: string]: L.LeafletEventHandlerFn } {
    return {
      move: this._throttledUpdate, // sensibly throttle updating while panning
      zoomanim: this._animateZoom, // applys the zoom animation to the <canvas>
      zoom: this._pinchZoom, // animate every zoom event for smoother pinch-zooming
      zoomstart: this._zoomStart, // flag starting a zoom to disable panning
      zoomend: this._zoomEnd,
      resize: this._resize
    };
  }

  getMapLibreGLMap(): MapLibreGLMap {
    return this._glMap;
  }

  getCanvas(): HTMLCanvasElement {
    return this._glMap.getCanvas();
  }

  getSize(): L.Point {
    return this._map.getSize().multiplyBy(1 + this.options.padding * 2);
  }

  getBounds(): L.LatLngBounds {
    const halfSize = this.getSize().multiplyBy(0.5);
    const center = this._map.latLngToContainerPoint(this._map.getCenter());
    return L.latLngBounds(
      this._map.containerPointToLatLng(center.subtract(halfSize)),
      this._map.containerPointToLatLng(center.add(halfSize))
    );
  }

  getContainer(): HTMLDivElement {
    return this._container;
  }

  getPaneName(): string {
    return this._map.getPane(this.options.pane) ? this.options.pane : 'tilePane';
  }

  private _roundPoint(p: L.Point): L.Point {
    return new L.Point(Math.round(p.x), Math.round(p.y));
  }

  private _initContainer() {
    const container = this._container = L.DomUtil.create('div', 'leaflet-gl-layer');
    const size = this.getSize();
    const offset = this._map.getSize().multiplyBy(this.options.padding);
    container.style.width  = size.x + 'px';
    container.style.height = size.y + 'px';
    const topLeft = this._map.containerPointToLayerPoint([0, 0]).subtract(offset);
    L.DomUtil.setPosition(container, this._roundPoint(topLeft));
  }

  private _initGL() {
    const center = this._map.getCenter();
    const options = L.extend({}, this.options, {
      container: this._container,
      center: [center.lng, center.lat],
      zoom: this._map.getZoom() - 1,
      attributionControl: false
    });
    this._glMap = new MapLibreGLMap(options);
    this._glMap.once('load', () => {
      this.fire('load');
    });
    this._glMap.setMaxBounds(null);
    this._transformGL(this._glMap);
    this._actualCanvas = this._glMap._canvas;
    const canvas = this._actualCanvas;
    L.DomUtil.addClass(canvas, 'leaflet-image-layer');
    L.DomUtil.addClass(canvas, 'leaflet-zoom-animated');
    if (this.options.interactive) {
      L.DomUtil.addClass(canvas, 'leaflet-interactive');
    }
    if (this.options.className) {
      L.DomUtil.addClass(canvas, this.options.className);
    }
  }

  private _update() {
    if (!this._map) {
      return;
    }
    this._offset = this._map.containerPointToLayerPoint([0, 0]);

    if (this._zooming) {
      return;
    }
    const size = this.getSize(),
      container = this._container,
      gl = this._glMap,
      offset = this._map.getSize().multiplyBy(this.options.padding),
      topLeft = this._map.containerPointToLayerPoint([0, 0]).subtract(offset);

    L.DomUtil.setPosition(container, this._roundPoint(topLeft));

    this._transformGL(gl);

    if (gl.transform.width !== size.x || gl.transform.height !== size.y) {
      container.style.width  = size.x + 'px';
      container.style.height = size.y + 'px';
      gl.resize();
    } else {
      gl._update();
    }
  }

  private _transformGL(gl: MapLibreGLMap) {
    const center = this._map.getCenter();
    const tr = gl._getTransformForUpdate();
    tr.setCenter(MapLibreGLLngLat.convert([center.lng, center.lat]));
    tr.setZoom(this._map.getZoom() - 1);
    gl.transform.apply(tr);
    gl._fireMoveEvents();
  }

  private _pinchZoom() {
    this._glMap.jumpTo({
      zoom: this._map.getZoom() - 1,
      center: this._map.getCenter()
    });
  }

  private _animateZoom(e: L.ZoomAnimEvent) {
    const scale = this._map.getZoomScale(e.zoom);
    const padding = this._map.getSize().multiplyBy(this.options.padding * scale);
    const viewHalf = this.getSize().divideBy(2);

    const topLeft = this._map.project(e.center, e.zoom)
    .subtract(viewHalf)
    .add((this._map as any)._getMapPanePos()
    .add(padding)).round();

    const offset = this._map.project(this._map.getBounds().getNorthWest(), e.zoom)
                         .subtract(topLeft);

    L.DomUtil.setTransform(
      this._actualCanvas,
      offset.subtract(this._offset),
      scale
    );
  }

  private _zoomStart() {
    this._zooming = true;
  }

  private _zoomEnd() {
    const scale = this._map.getZoomScale(this._map.getZoom());
    L.DomUtil.setTransform(
      this._actualCanvas,
      null,
      scale
    );
    this._zooming = false;
    this._update();
  }

  private _transitionEnd() {
    L.Util.requestAnimFrame(() => {
      const zoom = this._map.getZoom();
      const center = this._map.getCenter();
      const offset = this._map.latLngToContainerPoint(
        this._map.getBounds().getNorthWest()
      );

      L.DomUtil.setTransform(this._actualCanvas, offset, 1);

      this._glMap.once('moveend', () => {
        this._zoomEnd();
      });
      this._glMap.jumpTo({
        center: center,
        zoom: zoom - 1
      });
    });
  }

  private _resize() {
    this._transitionEnd();
  }

  private _loadAttribution(options: TB.MapLibreGL.LeafletMapLibreGLMapOptions): string {
    if (options.attributionControl !== false && typeof options.attributionControl?.customAttribution === 'string') {
      return options.attributionControl.customAttribution;
    }
    if (options.attributionControl !== false) {
      const style = options.style;
      if (typeof style !== 'string' && style?.sources) {
        return Object.keys(style.sources)
        .map((sourceId) => {
          const source = style.sources[sourceId];
          return (source && source.type !== 'video' && source.type !== 'image'
            && typeof source.attribution === 'string') ? source.attribution.trim() : null;
        })
        .filter(Boolean) // Remove null/undefined values
        .join(', ');
      }
    }
    return '';
  }

  private _prepareTransformRequest(options: TB.MapLibreGL.LeafletMapLibreGLMapOptions) {
    if (!options.transformRequest) {
      const style = options.style;
      if (typeof style !== 'string' && style.glyphs) {
        const glyphs = style.glyphs;
        const glyphsRegexString = glyphs.replace(/\//g, '\\/').replace(/\./g, '\\.').replace('{fontstack}', '(.*)').replace('{range}', '(.*)');
        const glyphsRegex = new RegExp(glyphsRegexString);
        options.transformRequest = (url, resourceType) => {
          if (resourceType === 'Glyphs' && glyphsRegex && glyphsRegex.test(url)) {
            const res = glyphsRegex.exec(url);
            if (res.length === 3) {
              const fontStack = res[1];
              const fonts = fontStack.split(',');
              if (fonts.length > 1) {
                const newFontStack = fonts[0];
                url = url.replace(fontStack, newFontStack);
              }
            }
          }
          return {url};
        };
      }
    }
  }
}

const mapLibreGLLayer = (options: TB.MapLibreGL.LeafletMapLibreGLMapOptions): TB.MapLibreGL.MapLibreGLLayer => {
  return new MapLibreGLLayer(options);
}

L.TB = L.TB || {
  SidebarControl,
  SidebarPaneControl,
  LayersControl,
  GroupsControl,
  TopToolbarButton,
  TopToolbarControl,
  ToolbarButton,
  ToolbarControl,
  BottomToolbarControl,
  Pattern,
  PatternElement,
  PatternShape,
  PatternRect,
  PatternPath,
  PatternImage,
  sidebar,
  sidebarPane,
  layers,
  groups,
  topToolbar,
  toolbar,
  bottomToolbar,
  TileLayer: {
    ChinaProvider
  },
  tileLayer: {
    chinaProvider
  },
  MapLibreGL: {
    MapLibreGLLayer,
    mapLibreGLLayer
  }
}
