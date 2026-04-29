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

import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  DataToValueType,
  GetValueAction,
  GetValueSettings,
  SetValueAction,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { Circle, Effect, Element, G, Gradient, Path, Runner, Svg, Text, Timeline } from '@svgdotjs/svg.js';
import '@svgdotjs/svg.filter.js';
import tinycolor from 'tinycolor2';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable, of, shareReplay } from 'rxjs';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { catchError, map, take } from 'rxjs/operators';
import { MatIconRegistry } from '@angular/material/icon';
import { isDefinedAndNotNull } from '@core/utils';

export enum PowerButtonLayout {
  default = 'default',
  simplified = 'simplified',
  outlined = 'outlined',
  default_volume = 'default_volume',
  simplified_volume = 'simplified_volume',
  outlined_volume = 'outlined_volume',
  default_icon = 'default_icon',
  simplified_icon = 'simplified_icon',
  outlined_icon = 'outlined_icon'
}

export const powerButtonLayouts = Object.keys(PowerButtonLayout) as PowerButtonLayout[];

export const powerButtonLayoutTranslations = new Map<PowerButtonLayout, string>(
  [
    [PowerButtonLayout.default, 'widgets.power-button.layout-default'],
    [PowerButtonLayout.simplified, 'widgets.power-button.layout-simplified'],
    [PowerButtonLayout.outlined, 'widgets.power-button.layout-outlined'],
    [PowerButtonLayout.default_volume, 'widgets.power-button.layout-default-volume'],
    [PowerButtonLayout.simplified_volume, 'widgets.power-button.layout-simplified-volume'],
    [PowerButtonLayout.outlined_volume, 'widgets.power-button.layout-outlined-volume'],
    [PowerButtonLayout.default_icon, 'widgets.power-button.layout-default-icon'],
    [PowerButtonLayout.simplified_icon, 'widgets.power-button.layout-simplified-icon'],
    [PowerButtonLayout.outlined_icon, 'widgets.power-button.layout-outlined-icon']
  ]
);

export const powerButtonLayoutImages = new Map<PowerButtonLayout, string>(
  [
    [PowerButtonLayout.default, 'assets/widget/power-button/default-layout.svg'],
    [PowerButtonLayout.simplified, 'assets/widget/power-button/simplified-layout.svg'],
    [PowerButtonLayout.outlined, 'assets/widget/power-button/outlined-layout.svg'],
    [PowerButtonLayout.default_volume, 'assets/widget/power-button/default-volume-layout.svg'],
    [PowerButtonLayout.simplified_volume, 'assets/widget/power-button/simplified-volume-layout.svg'],
    [PowerButtonLayout.outlined_volume, 'assets/widget/power-button/outlined-volume-layout.svg'],
    [PowerButtonLayout.default_icon, 'assets/widget/power-button/default-icon-layout.svg'],
    [PowerButtonLayout.simplified_icon, 'assets/widget/power-button/simplified-icon-layout.svg'],
    [PowerButtonLayout.outlined_icon, 'assets/widget/power-button/outlined-icon-layout.svg']
  ]
);

export interface ButtonIconSettings {
  showIcon: boolean;
  iconSize: number;
  iconSizeUnit: string;
  icon: string;
}

export interface PowerButtonWidgetSettings {
  initialState?: GetValueSettings<boolean>;
  disabledState?: GetValueSettings<boolean>;
  onUpdateState?: SetValueSettings;
  offUpdateState?: SetValueSettings;
  layout: PowerButtonLayout;
  onButtonIcon: ButtonIconSettings,
  offButtonIcon: ButtonIconSettings,
  mainColorOn: string;
  backgroundColorOn: string;
  mainColorOff: string;
  backgroundColorOff: string;
  mainColorDisabled: string;
  backgroundColorDisabled: string;
  background?: BackgroundSettings;
  padding?: string;
}

export const powerButtonDefaultSettings: PowerButtonWidgetSettings = {
  initialState: {
    action: GetValueAction.EXECUTE_RPC,
    defaultValue: false,
    executeRpc: {
      method: 'getState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  disabledState: {
    action: GetValueAction.DO_NOTHING,
    defaultValue: false,
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  onUpdateState: {
    action: SetValueAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SERVER_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    valueToData: {
      type: ValueToDataType.CONSTANT,
      constantValue: true,
      valueToDataFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  offUpdateState: {
    action: SetValueAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SERVER_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    valueToData: {
      type: ValueToDataType.CONSTANT,
      constantValue: false,
      valueToDataFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */ \n return value;'
    }
  },
  layout: PowerButtonLayout.default,
  onButtonIcon: {
    showIcon: false,
    iconSize: 32,
    iconSizeUnit: 'px',
    icon: 'power_settings_new'
  },
  offButtonIcon: {
    showIcon: false,
    iconSize: 32,
    iconSizeUnit: 'px',
    icon: 'power_settings_new'
  },
  mainColorOn: '#3F52DD',
  backgroundColorOn: '#FFFFFF',
  mainColorOff: '#A2A2A2',
  backgroundColorOff: '#FFFFFF',
  mainColorDisabled: 'rgba(0,0,0,0.12)',
  backgroundColorDisabled: '#FFFFFF',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};

interface PowerButtonColor {
  hex: string;
  opacity: number;
}

type PowerButtonState = 'on' | 'off' | 'disabled';

interface PowerButtonColorState {
  mainColor: PowerButtonColor;
  backgroundColor: PowerButtonColor;
}

interface ButtonsIconSettings {
  onButtonIcon: ButtonIconSettings;
  offButtonIcon: ButtonIconSettings;
}

type PowerButtonShapeColors = Record<PowerButtonState, PowerButtonColorState>;

const createPowerButtonShapeColors = (settings: PowerButtonWidgetSettings): PowerButtonShapeColors => {
  const mainColorOn = tinycolor(settings.mainColorOn);
  const backgroundColorOn = tinycolor(settings.backgroundColorOn);
  const mainColorOff = tinycolor(settings.mainColorOff);
  const backgroundColorOff = tinycolor(settings.backgroundColorOff);
  const mainColorDisabled = tinycolor(settings.mainColorDisabled);
  const backgroundColorDisabled = tinycolor(settings.backgroundColorDisabled);
  return {
    on: {
      mainColor: {hex: mainColorOn.toHexString(), opacity: mainColorOn.getAlpha()},
      backgroundColor: {hex: backgroundColorOn.toHexString(), opacity: backgroundColorOn.getAlpha()},
    },
    off: {
      mainColor: {hex: mainColorOff.toHexString(), opacity: mainColorOff.getAlpha()},
      backgroundColor: {hex: backgroundColorOff.toHexString(), opacity: backgroundColorOff.getAlpha()},
    },
    disabled: {
      mainColor: {hex: mainColorDisabled.toHexString(), opacity: mainColorDisabled.getAlpha()},
      backgroundColor: {hex: backgroundColorDisabled.toHexString(), opacity: backgroundColorDisabled.getAlpha()},
    }
  };
};

export const powerButtonShapeSize = 110;
const cx = powerButtonShapeSize / 2;
const cy = powerButtonShapeSize / 2;

const powerCircle = 'M13 4.67063C13 3.65748 12.0377 2.91866 11.0946 3.28889C4.59815 5.83928 0 12.1545 0 19.5412C0 29.1835 ' +
  '7.83502 37.0001 17.5 37.0001C27.165 37.0001 35 29.1835 35 19.5412C35 12.1545 30.4019 5.83928 23.9054 3.28889C22.9623 2.91866 22 ' +
  '3.65748 22 4.67063C22 5.33931 22.434 5.92434 23.0519 6.17991C28.3077 8.35375 32 13.5209 32 19.5412C32 27.52 25.5148 34.0001 17.5 ' +
  '34.0001C9.48521 34.0001 3 27.52 3 19.5412C3 13.5209 6.69234 8.35374 11.9481 6.17991C12.566 5.92434 13 5.33931 13 4.67063Z';
const powerLine = 'M16.5 1C16.5 0.447716 16.9477 0 17.5 0C18.0523 0 18.5 0.447715 18.5 1V17C18.5 ' +
  '17.5523 18.0523 18 17.5 18C16.9477 18 16.5 17.5523 16.5 17V1Z';

const powerCircleStroke = 'M12 2.95698C12 1.45236 10.4775 0.438524 9.19424 1.22416C3.68417 4.59764 0 10.7283 0 17.7316C0 ' +
  '28.3732 8.50659 37 19 37C29.4934 37 38 28.3732 38 17.7316C38 10.7283 34.3158 4.59764 28.8058 1.22416C27.5225 ' +
  '0.438524 26 1.45236 26 2.95698C26 3.73878 26.4365 4.44718 27.0911 4.87461C31.2354 7.58066 34 12.3083 34 ' +
  '17.7316C34 26.2172 27.2316 33 19 33C10.7684 33 4 26.2172 4 17.7316C4 12.3084 6.76462 7.58066 10.9089 ' +
  '4.87461C11.5635 4.44718 12 3.73878 12 2.95698Z';
const powerLineStroke = 'M0 2.5C0 1.11929 1.11929 0 2.5 0C3.88071 0 5 1.11929 5 2.5V15.5C5 16.8807 3.88071 18 ' +
  '2.5 18C1.11929 18 0 16.8807 0 15.5V2.5Z';

const powerButtonAnimation = (element: Element): Runner => element.animate(200, 0, 'now');

export abstract class PowerButtonShape {

  static fromSettings(ctx: WidgetContext,
                      svgShape: Svg,
                      iconRegistry: MatIconRegistry,
                      settings: PowerButtonWidgetSettings,
                      value: boolean,
                      disabled: boolean,
                      onClick: () => void): PowerButtonShape {
    switch (settings.layout) {
      case PowerButtonLayout.default:
        return new DefaultPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.simplified:
        return new SimplifiedPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.outlined:
        return new OutlinedPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.default_volume:
        return new DefaultVolumePowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.simplified_volume:
        return new SimplifiedVolumePowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.outlined_volume:
        return new OutlinedVolumePowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.default_icon:
        return new DefaultIconPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.simplified_icon:
        return new SimplifiedIconPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
      case PowerButtonLayout.outlined_icon:
        return new OutlinedIconPowerButtonShape(ctx, svgShape, iconRegistry, settings, value, disabled, onClick);
    }
  }

  protected readonly colors: PowerButtonShapeColors;
  protected readonly icons: ButtonsIconSettings;
  protected readonly onLabel: string;
  protected readonly offLabel: string;

  protected backgroundShape: Circle;
  protected hoverShape: Circle;
  protected hovered = false;
  protected pressed = false;
  protected forcePressed = false;

  protected offPowerSymbolIcon: Element;
  protected onPowerSymbolIcon: Element;
  protected offLabelShape: Text;
  protected onLabelShape: Text;

  protected offPowerSymbolCircle: Path;
  protected offPowerSymbolLine: Path;
  protected onPowerSymbolCircle: Path;
  protected onPowerSymbolLine: Path;

  private onIcon$: Observable<Element>;
  private offIcon$: Observable<Element>;
  protected onIconOffsetX: number = 0;
  protected onIconOffsetY: number = 0;

  protected constructor(protected widgetContext: WidgetContext,
                        protected svgShape: Svg,
                        protected iconRegistry: MatIconRegistry,
                        protected settings: PowerButtonWidgetSettings,
                        protected value: boolean,
                        protected disabled: boolean,
                        protected onClick: () => void) {
    this.colors = createPowerButtonShapeColors(this.settings);
    this.icons = {onButtonIcon: this.settings.onButtonIcon, offButtonIcon: this.settings.offButtonIcon};
    this.onLabel = this.widgetContext.translate.instant('widgets.power-button.on-label').toUpperCase();
    this.offLabel = this.widgetContext.translate.instant('widgets.power-button.off-label').toUpperCase();
    this._drawShape();
  }

  public createIconElement(icon: string, size: number): Observable<Element> {
    const isSvg = isSvgIcon(icon);
    if (isSvg) {
      const [namespace, iconName] = splitIconName(icon);
      return this.iconRegistry
        .getNamedSvgIcon(iconName, namespace)
        .pipe(
          take(1),
          map((svgElement) => {
            const element = new Element(svgElement.firstChild);
            const iconGroup = this.svgShape.group();
            element.addTo(iconGroup);
            const box = element.bbox();
            const scale = size / box.height;
            element.scale(scale);
            const scaledBox = iconGroup.bbox();
            iconGroup.translate(-scaledBox.cx, -scaledBox.cy);
            return iconGroup;
          }),
          catchError(() => of(null)
          ));
    } else {
      const iconName = splitIconName(icon)[1];
      const textElement = this.svgShape.text(iconName);
      const fontSetClasses = (
        this.iconRegistry.getDefaultFontSetClass()
      ).filter(className => className.length > 0);
      fontSetClasses.forEach(className => textElement.addClass(className));
      textElement.font({size: `${size}px`});
      textElement.attr({
        style: `font-size: ${size}px`,
        'text-anchor': 'start'
      });
      const tspan = textElement.first();
      tspan.attr({
        'dominant-baseline': 'hanging'
      });
      return of(textElement);
    }
  }

  public setValue(value: boolean) {
    if (this.value !== value) {
      this.value = value;
      this._drawState();
    }
  }

  public setDisabled(disabled: boolean) {
    if (this.disabled !== disabled) {
      this.disabled = disabled;
      this._drawState();
    }
  }

  public setPressed(pressed: boolean) {
    if (this.forcePressed !== pressed) {
      this.forcePressed = pressed;
      if (this.forcePressed && !this.pressed) {
        this.onPressStart();
      } else if (!this.forcePressed && !this.pressed) {
        this.onPressEnd();
      }
    }
  }

  public drawOffShape(centerGroup: G, label: boolean, labelWeight?: string, circleStroke?: boolean) {
    if (this.icons.offButtonIcon.showIcon) {
      this.offIcon$ = this.createIconElement(this.icons.offButtonIcon.icon, this.icons.offButtonIcon.iconSize)
        .pipe(shareReplay(1));

      this.offIcon$.pipe(take(1)).subscribe(icon => {
        this.offPowerSymbolIcon = icon;
        icon.translate(cx, cy);
        centerGroup.add(icon);
      });

    } else {
      if (label) {
        this.offLabelShape = this.createOffLabel(labelWeight).addTo(centerGroup);
      } else {
        this.offPowerSymbolCircle = this.svgShape.path(circleStroke ? powerCircle : powerCircleStroke).center(cx, cy).addTo(centerGroup);
        this.offPowerSymbolLine = this.svgShape.path(circleStroke ? powerLine : powerLineStroke).center(cx, cy-12).addTo(centerGroup);
      }
    }
  }

  public drawOnShape(onCenterGroup?: G, label?: boolean, labelWeight?: string, circleStroke?: boolean, mask?: Circle) {
    if (this.icons.onButtonIcon.showIcon) {
      this.onIcon$ = this.createIconElement(this.icons.onButtonIcon.icon, this.icons.onButtonIcon.iconSize)
        .pipe(shareReplay(1));

      this.onIcon$.subscribe(icon => {
        const iconBox = icon.bbox();
        this.onIconOffsetX = iconBox.cx;
        this.onIconOffsetY = iconBox.cy;
        this.onPowerSymbolIcon = icon.translate(cx, cy);
        if (isDefinedAndNotNull(onCenterGroup)) {
          onCenterGroup.add(this.onPowerSymbolIcon);
        }
        if (isDefinedAndNotNull(mask)) {
          this.createMask(mask, [this.onPowerSymbolIcon]);
        }
      });
    } else {
      if (label) {
        this.onLabelShape = this.createOnLabel(labelWeight);
        if (isDefinedAndNotNull(onCenterGroup)) {
          this.onLabelShape.addTo(onCenterGroup);
        }
        if (isDefinedAndNotNull(mask)) {
          this.createMask(mask, [this.onLabelShape]);
        }
      } else {
        this.onPowerSymbolCircle = this.svgShape.path(circleStroke ? powerCircle : powerCircleStroke).center(cx, cy);
        this.onPowerSymbolLine = this.svgShape.path(circleStroke ? powerLine : powerLineStroke).center(cx, cy-12);
        if (isDefinedAndNotNull(onCenterGroup)) {
          this.onPowerSymbolCircle.addTo(onCenterGroup);
          this.onPowerSymbolLine.addTo(onCenterGroup);
        }
        if (isDefinedAndNotNull(mask)) {
          this.createMask(mask, [this.onPowerSymbolCircle, this.onPowerSymbolLine]);
        }
      }
    }
  }

  public onCenterTimeLine(timeline: Timeline, label: boolean) {
    if (this.icons.onButtonIcon.showIcon) {
      if (this.onIcon$) {
        this.onIcon$.subscribe(icon => icon.timeline(timeline));
      }
    } else {
      if (label) {
        this.onLabelShape.timeline(timeline);
      } else {
        this.onPowerSymbolCircle.timeline(timeline);
        this.onPowerSymbolLine.timeline(timeline);
      }
    }
  }

  public offCenterColor(mainColor: PowerButtonColor, label: boolean) {
    if (this.icons.offButtonIcon.showIcon) {
      this.offIcon$.subscribe(icon => icon.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity}))
    } else {
      if (label) {
        this.offLabelShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
      } else {
        this.offPowerSymbolCircle.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
        this.offPowerSymbolLine.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
      }
    }
  }

  public onCenterColor(mainColor: PowerButtonColor, label: boolean) {
    if (this.icons.onButtonIcon.showIcon) {
      this.onIcon$.subscribe((icon)=> icon.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity}))
    } else {
      if (label) {
        this.onLabelShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
      } else {
        this.onPowerSymbolCircle.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
        this.onPowerSymbolLine.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
      }
    }
  }

  public buttonAnimation(scale: number, label: boolean) {
    if (this.icons.onButtonIcon.showIcon) {
      const translateX = cx - this.onIconOffsetX;
      const translateY = cy - this.onIconOffsetY;
      powerButtonAnimation(this.onPowerSymbolIcon).transform({
        scale: scale,
        translate: [translateX, translateY]
      });
    } else {
      if (label) {
        powerButtonAnimation(this.onLabelShape).transform({scale, origin: {x: cx, y: cy}});
      } else {
        powerButtonAnimation(this.onPowerSymbolCircle).transform({scale, origin: {x: cx, y: cy}});
        powerButtonAnimation(this.onPowerSymbolLine).transform({scale, origin: {x: cx, y: cy}});
      }
    }
  }

  private _drawShape() {

    this.backgroundShape = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});

    this.drawShape();

    this.hoverShape = this.svgShape.circle(powerButtonShapeSize).center(cx, cy).addClass('tb-hover-circle')
    .fill({color: '#000000', opacity: 0});
    this.hoverShape.on('mouseover', () => {
      this.hovered = true;
      if (!this.disabled) {
        this.hoverShape.timeline().finish();
        this.hoverShape.animate(200).attr({'fill-opacity': 0.06});
      }
    });
    this.hoverShape.on('mouseout', () => {
      this.hovered = false;
      this.hoverShape.timeline().finish();
      this.hoverShape.animate(200).attr({'fill-opacity': 0});
      this._cancelPressed();
    });
    this.hoverShape.on('touchmove', (event: TouchEvent) => {
      const touch = event.touches[0];
      const element = document.elementFromPoint(touch.pageX,touch.pageY);
      if (this.hoverShape.node !== element) {
        this._cancelPressed();
      }
    });
    this.hoverShape.on('touchcancel', () => {
      this._cancelPressed();
    });
    this.hoverShape.on('mousedown touchstart', (event: Event) => {
      if (event.type === 'mousedown') {
        if ((event as MouseEvent).button !== 0) {
          return;
        }
      }
      if (!this.disabled && !this.pressed) {
        this.pressed = true;
        if (!this.forcePressed) {
          this.onPressStart();
        }
      }
    });
    this.hoverShape.on('mouseup touchend touchcancel', () => {
      if (this.pressed && !this.disabled) {
        this.onClick();
      }
      this._cancelPressed();
    });
    this._drawState();
  }

  private _cancelPressed() {
    if (this.pressed) {
      this.pressed = false;
      if (!this.forcePressed) {
        this.onPressEnd();
      }
    }
  }

  private _drawState() {
    let colorState: PowerButtonColorState;
    if (this.disabled) {
      colorState = this.colors.disabled;
    } else {
      colorState = this.value ? this.colors.on : this.colors.off;
    }
    this.drawBackgroundState(colorState.backgroundColor);
    this.drawColorState(colorState.mainColor);
    if (this.value) {
      this.drawOn();
    } else {
      this.drawOff();
    }
    if (this.disabled) {
      this.hoverShape.timeline().finish();
      this.hoverShape.attr({'fill-opacity': 0});
    } else if (this.hovered) {
      this.hoverShape.timeline().finish();
      this.hoverShape.animate(200).attr({'fill-opacity': 0.06});
    }
  }

  private drawBackgroundState(backgroundColor: PowerButtonColor) {
    this.backgroundShape.attr({ fill: backgroundColor.hex, 'fill-opacity': backgroundColor.opacity});
  }

  protected drawShape() {}

  protected drawColorState(_mainColor: PowerButtonColor) {}

  protected drawOff() {}

  protected drawOn() {}

  protected onPressStart() {}

  protected onPressEnd() {}

  protected createMask(shape: Element, maskElements: Element[]) {
    const mask =
      this.svgShape.mask().add(this.svgShape.rect().width('100%').height('100%').fill('#fff'));
    maskElements.forEach(e => {
      mask.add(e.fill('#000').attr({'fill-opacity': 1}));
    });
    shape.maskWith(mask);
  }

  protected createOnLabel(fontWeight = '500'): Text {
    return this.createLabel(this.onLabel, fontWeight);
  }

  protected createOffLabel(fontWeight = '500'): Text {
    return this.createLabel(this.offLabel, fontWeight);
  }

  private createLabel(text: string, fontWeight = '500'): Text {
    return this.svgShape.text(text).font({
      family: 'Roboto',
      weight: fontWeight,
      style: 'normal',
      size: '22px'
    }).attr({x: '50%', y: '50%', 'text-anchor': 'middle', 'dominant-baseline': 'middle'});
  }

}

class InnerShadowCircle {

  private shadowCircle: Circle;
  private blurEffect: Effect;
  private offsetEffect: Effect;
  private floodEffect: Effect;

  constructor(private svgShape: Svg,
              private diameter: number,
              private centerX: number,
              private centerY: number,
              private blur = 6,
              private shadowOpacity = 0.6,
              private dx = 0,
              private dy = 0,
              private shadowColor = '#000') {

    this.shadowCircle = this.svgShape.circle(this.diameter).center(this.centerX, this.centerY)
    .fill({color: '#fff', opacity: 1}).stroke({width: 0});

    this.shadowCircle.filterWith(add => {
      add.x('-50%').y('-50%').width('200%').height('200%');
      let effect: Effect = add.componentTransfer(components => {
        components.funcA({ type: 'table', tableValues: '1 0' });
      });
      effect = effect.gaussianBlur(this.blur, this.blur).attr({stdDeviation: this.blur});
      this.blurEffect = effect;
      effect = effect.offset(this.dx, this.dy);
      this.offsetEffect = effect;
      effect = effect.flood(this.shadowColor, this.shadowOpacity);
      this.floodEffect = effect;
      effect = effect.composite(this.offsetEffect, 'in');
      effect.composite(add.$sourceAlpha, 'in');
      add.merge(m => {
        m.mergeNode();
        m.mergeNode();
      });
    });
  }

  public timeline(tl: Timeline): void {
    this.blurEffect.timeline(tl);
    this.offsetEffect.timeline(tl);
    this.floodEffect.timeline(tl);
  }

  public animate(blur: number, opacity: number, dx = 0, dy = 0): Runner {
    powerButtonAnimation(this.blurEffect).attr({stdDeviation: blur});
    powerButtonAnimation(this.offsetEffect).attr({dx, dy});
    return powerButtonAnimation(this.floodEffect).attr({'flood-opacity': opacity});
  }

  public animateRestore(): Runner {
    return this.animate(this.blur, this.shadowOpacity, this.dx, this.dy);
  }

  public show(): void {
    this.shadowCircle.show();
  }

  public hide(): void {
    this.shadowCircle.hide();
  }

}

class DefaultPowerButtonShape extends PowerButtonShape {

  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private onCircleShape: Circle;
  private pressedShadow: InnerShadowCircle;
  private pressedTimeline: Timeline;
  private centerGroup: G;

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
                       .fill({opacity: 0}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.centerGroup = this.svgShape.group();
    this.drawOffShape(this.centerGroup, true);
    this.onCircleShape = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy);
    this.drawOnShape(null, true, '', false, this.onCircleShape);
    this.pressedShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 20, cx, cy, 0, 0);

    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.onCenterTimeLine(this.pressedTimeline, true);
    this.pressedShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor) {
    this.outerBorder.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.offCenterColor(mainColor, true);
    this.onCircleShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
  }

  protected drawOff() {
    this.outerBorderMask.radius((powerButtonShapeSize - 20)/2);
    this.onCircleShape.hide();
    this.centerGroup.show();
  }

  protected drawOn() {
    this.outerBorderMask.radius((powerButtonShapeSize - 2)/2);
    this.centerGroup.hide();
    this.onCircleShape.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    const pressedScale = 0.75;
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    this.buttonAnimation(pressedScale, true);
    this.pressedShadow.animate(6, 0.6);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    this.buttonAnimation(1, true);
    this.pressedShadow.animateRestore();
  }

}

class SimplifiedPowerButtonShape extends PowerButtonShape {

  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private onCircleShape: Circle;
  private pressedShadow: InnerShadowCircle;
  private pressedTimeline: Timeline;
  private centerGroup: G;

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 4).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.centerGroup = this.svgShape.group();
    this.drawOffShape(this.centerGroup, true);
    this.onCircleShape = this.svgShape.circle(powerButtonShapeSize).center(cx, cy);
    this.drawOnShape(null, true, '', false, this.onCircleShape)
    this.pressedShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 4, cx, cy, 0, 0);

    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.onCenterTimeLine(this.pressedTimeline, true);
    this.pressedShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor) {
    this.outerBorder.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.onCircleShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.offCenterColor(mainColor, true);
  }

  protected drawOff() {
    this.onCircleShape.hide();
    this.outerBorder.show();
    this.centerGroup.show();
  }

  protected drawOn() {
    this.centerGroup.hide();
    this.outerBorder.hide();
    this.onCircleShape.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    const pressedScale = 0.75;
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    this.buttonAnimation(pressedScale, true);
    this.pressedShadow.animate(6, 0.6);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    this.buttonAnimation(1, true);
    this.pressedShadow.animateRestore();
  }
}

class OutlinedPowerButtonShape extends PowerButtonShape {
  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private innerBorder: Circle;
  private innerBorderMask: Circle;
  private onCircleShape: Circle;
  private pressedShadow: InnerShadowCircle;
  private pressedTimeline: Timeline;
  private centerGroup: G;
  private onCenterGroup: G;

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 2).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.innerBorder = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.innerBorderMask = this.svgShape.circle(powerButtonShapeSize - 24).center(cx, cy);
    this.createMask(this.innerBorder, [this.innerBorderMask]);
    this.centerGroup = this.svgShape.group();
    this.drawOffShape(this.centerGroup, true);
    this.onCenterGroup = this.svgShape.group();
    this.onCircleShape = this.svgShape.circle(powerButtonShapeSize - 28).center(cx, cy).addTo(this.onCenterGroup);
    this.drawOnShape(null, true, '', false, this.onCircleShape)
    this.pressedShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 24, cx, cy, 0, 0);

    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.onCenterGroup.timeline(this.pressedTimeline);
    this.onCenterTimeLine(this.pressedTimeline, true);
    this.pressedShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor) {
    this.outerBorder.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.innerBorder.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.offCenterColor(mainColor, true);
    this.onCircleShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
  }

  protected drawOff() {
    this.onCenterGroup.hide();
    this.centerGroup.show();
  }

  protected drawOn() {
    this.centerGroup.hide();
    this.onCenterGroup.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    const pressedScale = 0.75;
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    powerButtonAnimation(this.onCenterGroup).transform({scale: 0.98});
    this.buttonAnimation(pressedScale / 0.98, true);
    this.pressedShadow.animate(6, 0.6);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    powerButtonAnimation(this.onCenterGroup).transform({scale: 1});
    this.buttonAnimation(1, true);
    this.pressedShadow.animateRestore();
  }
}

class DefaultVolumePowerButtonShape extends PowerButtonShape {
  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private outerBorderGradient: Gradient;
  private innerBorder: Circle;
  private innerBorderMask: Circle;
  private innerBorderGradient: Gradient;
  private innerShadow: InnerShadowCircle;
  //private innerShadowGradient: Gradient;
  //private innerShadowGradientStop: Stop;
  protected onCircleShape: Circle;
  private pressedTimeline: Timeline;
  private centerGroup: G;


  protected drawOffCenter(centerGroup: G) {
    this.drawOffShape(centerGroup, true, '400');
  }

  protected drawOnCenter() {
    this.drawOnShape(null, true, '400', false, this.onCircleShape);
  }

  protected addOnCenterTimeLine(pressedTimeline: Timeline) {
    this.onCenterTimeLine(pressedTimeline, true);
  }

  protected drawOffCenterColor(mainColor: PowerButtonColor) {
    this.offCenterColor(mainColor, true);
  }

  protected onCenterAnimation(scale: number) {
    this.buttonAnimation(scale, true);
  }

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.outerBorderGradient = this.svgShape.gradient('linear', (add) => {
      add.stop(0, '#CCCCCC', 1);
      add.stop(1, '#FFFFFF', 1);
    }).from(0.268, 0.92).to(0.832, 0.1188);
    this.innerBorder = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.innerBorderMask = this.svgShape.circle(powerButtonShapeSize - 24).center(cx, cy);
    this.createMask(this.innerBorder, [this.innerBorderMask]);
    this.innerBorderGradient = this.svgShape.gradient('linear', (add) => {
      add.stop(0, '#CCCCCC', 1);
      add.stop(1, '#FFFFFF', 1);
    }).from(0.832, 0.1188).to(0.268, 0.92);
    this.centerGroup = this.svgShape.group();
    this.drawOffCenter(this.centerGroup);
    this.onCircleShape = this.svgShape.circle(powerButtonShapeSize - 24).center(cx, cy);
    this.drawOnCenter();
    this.innerShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 24, cx, cy, 3, 0.3);

    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.addOnCenterTimeLine(this.pressedTimeline);
    this.innerShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor){
    if (this.disabled) {
      this.backgroundShape.removeClass('tb-small-shadow');
      if (!this.forcePressed) {
        this.innerShadow.hide();
      }
      this.outerBorder.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
      this.innerBorder.attr({fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    } else {
      this.backgroundShape.addClass('tb-small-shadow');
      this.innerShadow.show();
      this.outerBorder.fill(this.outerBorderGradient);
      this.outerBorder.attr({ 'fill-opacity': 1 });
      this.innerBorder.fill(this.innerBorderGradient);
      this.innerBorder.attr({ 'fill-opacity': 1 });
    }
    this.drawOffCenterColor(mainColor);
    this.onCircleShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
  }

  protected drawOff() {
    this.onCircleShape.hide();
    this.centerGroup.show();
    this.innerBorder.show();
  }

  protected drawOn() {
    if (this.disabled) {
      this.innerBorder.hide();
    } else {
      this.innerBorder.show();
    }
    this.centerGroup.hide();
    this.onCircleShape.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    this.innerShadow.show();
    const pressedScale = 0.75;
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    this.onCenterAnimation(pressedScale);
    this.innerShadow.animate(6, 0.6);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    this.onCenterAnimation(1);
    this.innerShadow.animateRestore().after(() => {
      if (this.disabled) {
        this.innerShadow.hide();
      }
    });
  }

}

class DefaultIconPowerButtonShape extends DefaultVolumePowerButtonShape {

  protected drawOffCenter(centerGroup: G) {
    this.drawOffShape(centerGroup, false);
  }

  protected drawOnCenter() {
    this.drawOnShape(null, false, '', false, this.onCircleShape);
  }

  protected addOnCenterTimeLine(pressedTimeline: Timeline) {
    this.onCenterTimeLine(pressedTimeline, false);
  }

  protected drawOffCenterColor(mainColor: PowerButtonColor) {
    this.offCenterColor(mainColor, false);
  }

  protected onCenterAnimation(scale: number) {
    this.buttonAnimation(scale, false);
  }
}

class SimplifiedVolumePowerButtonShape extends PowerButtonShape {

  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private innerShadow: InnerShadowCircle;
  private pressedShadow: InnerShadowCircle;
  private pressedTimeline: Timeline;
  private centerGroup: G;
  private onCenterGroup: G;

  protected drawCenterGroup(centerGroup: G, onCenterGroup: G) {
    this.drawOffShape(centerGroup, true);
    this.drawOnShape(onCenterGroup, true);
  }

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({color: '#FAFAFA', opacity: 1}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 4).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.centerGroup = this.svgShape.group();
    this.onCenterGroup = this.svgShape.group();
    this.drawCenterGroup(this.centerGroup, this.onCenterGroup);
    this.innerShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 4, cx, cy, 3, 0.3);
    this.pressedShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 4, cx, cy, 0, 0);
    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.onCenterGroup.timeline(this.pressedTimeline);
    this.pressedShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor){
    this.offCenterColor(mainColor, true);
    this.onCenterColor(mainColor, true);
  }

  protected drawOff() {
    if (!this.pressed) {
      this.backgroundShape.addClass('tb-shadow');
    }
    this.innerShadow.hide();
    this.onCenterGroup.hide();
    this.centerGroup.show();
  }

  protected drawOn() {
    this.backgroundShape.removeClass('tb-shadow');
    this.centerGroup.hide();
    this.onCenterGroup.show();
    this.innerShadow.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    const pressedScale = 0.75;
    if (!this.value) {
      this.backgroundShape.removeClass('tb-shadow');
    }
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    powerButtonAnimation(this.onCenterGroup).transform({scale: pressedScale, origin: {x: cx, y: cy}});
    this.pressedShadow.animate(8, 0.4);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    powerButtonAnimation(this.onCenterGroup).transform({scale: 1, origin: {x: cx, y: cy}});
    this.pressedShadow.animateRestore().after(() => {
      if (!this.value) {
        this.backgroundShape.addClass('tb-shadow');
      }
    });
  }
}

class SimplifiedIconPowerButtonShape extends SimplifiedVolumePowerButtonShape {

  protected drawCenterGroup(centerGroup: G, onCenterGroup: G) {
    this.drawOffShape(centerGroup, false);
    this.drawOnShape(onCenterGroup, false, '', false);
  }

  protected drawColorState(mainColor: PowerButtonColor) {
    this.offCenterColor(mainColor, false);
    this.onCenterColor(mainColor, false);
  }
}

class OutlinedVolumePowerButtonShape extends PowerButtonShape {
  private outerBorder: Circle;
  private outerBorderMask: Circle;
  private outerBorderGradient: Gradient;
  private innerBorder: Circle;
  private innerBorderMask: Circle;
  protected onCircleShape: Circle;
  private pressedShadow: InnerShadowCircle;
  private pressedTimeline: Timeline;
  private centerGroup: G;
  private onCenterGroup: G;

  protected drawOffCenter(centerGroup: G) {
    this.drawOffShape(centerGroup, true, '800');
  }

  protected drawOnCenter() {
    this.drawOnShape(null, true, '800', false, this.onCircleShape);
  }

  protected addOnCenterTimeLine(pressedTimeline: Timeline) {
    this.onCenterTimeLine(pressedTimeline, true);
  }

  protected drawOffCenterColor(mainColor: PowerButtonColor) {
    this.offCenterColor(mainColor, true);
  }

  protected onCenterAnimation(scale: number) {
    this.buttonAnimation(scale, true);
  }

  protected drawShape() {
    this.outerBorder = this.svgShape.circle(powerButtonShapeSize).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.outerBorderMask = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy);
    this.createMask(this.outerBorder, [this.outerBorderMask]);
    this.outerBorderGradient = this.svgShape.gradient('linear', (add) => {
      add.stop(0, '#CCCCCC', 1);
      add.stop(1, '#FFFFFF', 1);
    }).from(0.268, 0.92).to(0.832, 0.1188);
    this.innerBorder = this.svgShape.circle(powerButtonShapeSize - 20).center(cx, cy)
    .fill({opacity: 0}).stroke({width: 0});
    this.innerBorderMask = this.svgShape.circle(powerButtonShapeSize - 30).center(cx, cy);
    this.createMask(this.innerBorder, [this.innerBorderMask]);
    this.centerGroup = this.svgShape.group();
    this.drawOffCenter(this.centerGroup);
    this.onCenterGroup = this.svgShape.group();
    this.onCircleShape = this.svgShape.circle(powerButtonShapeSize - 30).center(cx, cy)
    .addTo(this.onCenterGroup);
    this.drawOnCenter();
    this.pressedShadow = new InnerShadowCircle(this.svgShape, powerButtonShapeSize - 30, cx, cy, 0, 0);
    this.backgroundShape.addClass('tb-small-shadow');

    this.pressedTimeline = new Timeline();
    this.centerGroup.timeline(this.pressedTimeline);
    this.onCenterGroup.timeline(this.pressedTimeline);
    this.addOnCenterTimeLine(this.pressedTimeline);
    this.pressedShadow.timeline(this.pressedTimeline);
  }

  protected drawColorState(mainColor: PowerButtonColor){
    if (this.disabled) {
      this.outerBorder.attr({ fill: '#000000', 'fill-opacity': 0.03});
    } else {
      this.outerBorder.fill(this.outerBorderGradient);
      this.outerBorder.attr({ 'fill-opacity': 1 });
    }
    this.innerBorder.attr({fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
    this.drawOffCenterColor(mainColor);
    this.onCircleShape.attr({ fill: mainColor.hex, 'fill-opacity': mainColor.opacity});
  }

  protected drawOff() {
    this.onCenterGroup.hide();
    this.centerGroup.show();
    this.innerBorder.show();
  }

  protected drawOn() {
    this.innerBorder.hide();
    this.centerGroup.hide();
    this.onCenterGroup.show();
  }

  protected onPressStart() {
    this.pressedTimeline.finish();
    const pressedScale = 0.75;
    powerButtonAnimation(this.centerGroup).transform({scale: pressedScale});
    powerButtonAnimation(this.onCenterGroup).transform({scale: 0.98});
    this.onCenterAnimation(pressedScale / 0.98);
    this.pressedShadow.animate(6, 0.6);
  }

  protected onPressEnd() {
    this.pressedTimeline.finish();
    powerButtonAnimation(this.centerGroup).transform({scale: 1});
    powerButtonAnimation(this.onCenterGroup).transform({scale: 1});
    this.onCenterAnimation(1);
    this.pressedShadow.animateRestore();
  }

}

class OutlinedIconPowerButtonShape extends OutlinedVolumePowerButtonShape {

  protected drawOffCenter(centerGroup: G) {
    this.drawOffShape(centerGroup, false, '', true);
  }

  protected drawOnCenter() {
    this.drawOnShape(null, false, '', true, this.onCircleShape);
  }

  protected addOnCenterTimeLine(pressedTimeline: Timeline) {
    this.onCenterTimeLine(pressedTimeline, false);
  }

  protected drawOffCenterColor(mainColor: PowerButtonColor) {
    this.offCenterColor(mainColor, false);
  }

  protected onCenterAnimation(scale: number) {
    this.buttonAnimation(scale, false);
  }
}
