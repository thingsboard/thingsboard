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

import { cssUnit } from '@shared/models/widget-settings.models';
import tinycolor from 'tinycolor2';

const defaultMainColor = '#3F52DD';
const defaultBackgroundColor = '#FFFFFF';

const hoveredFilledDarkenAmount = 6;
const pressedFilledDarkenAmount = 12;
const activatedFilledDarkenAmount = 12;
const pressedRippleFilledDarkenAmount = 18;

export const defaultMainColorDisabled = 'rgba(0, 0, 0, 0.38)';
export const defaultBackgroundColorDisabled = 'rgba(0, 0, 0, 0.03)';

const defaultBoxShadowColor = 'rgba(0, 0, 0, 0.08)';
const defaultDisabledBoxShadowColor = 'rgba(0, 0, 0, 0)';

export enum WidgetButtonType {
  outlined = 'outlined',
  filled = 'filled',
  underlined = 'underlined',
  basic = 'basic'
}

export const widgetButtonTypes = Object.keys(WidgetButtonType) as WidgetButtonType[];

export const widgetButtonTypeTranslations = new Map<WidgetButtonType, string>(
  [
    [WidgetButtonType.outlined, 'widgets.button.outlined'],
    [WidgetButtonType.filled, 'widgets.button.filled'],
    [WidgetButtonType.underlined, 'widgets.button.underlined'],
    [WidgetButtonType.basic, 'widgets.button.basic']
  ]
);

export const widgetButtonTypeImages = new Map<WidgetButtonType, string>(
  [
    [WidgetButtonType.outlined, 'assets/widget/button/outlined.svg'],
    [WidgetButtonType.filled, 'assets/widget/button/filled.svg'],
    [WidgetButtonType.underlined, 'assets/widget/button/underlined.svg'],
    [WidgetButtonType.basic, 'assets/widget/button/basic.svg']
  ]
);

export enum WidgetButtonState {
  enabled = 'enabled',
  hovered = 'hovered',
  pressed = 'pressed',
  activated = 'activated',
  disabled = 'disabled'
}

export const widgetButtonStates = Object.keys(WidgetButtonState) as WidgetButtonState[];

export const widgetButtonStatesTranslations = new Map<WidgetButtonState, string>(
  [
    [WidgetButtonState.enabled, 'widgets.button-state.enabled'],
    [WidgetButtonState.hovered, 'widgets.button-state.hovered'],
    [WidgetButtonState.pressed, 'widgets.button-state.pressed'],
    [WidgetButtonState.activated, 'widgets.button-state.activated'],
    [WidgetButtonState.disabled, 'widgets.button-state.disabled']
  ]
);

export interface WidgetButtonCustomStyle {
  overrideMainColor?: boolean;
  mainColor?: string;
  overrideBackgroundColor?: boolean;
  backgroundColor?: string;
  overrideDropShadow?: boolean;
  dropShadow?: boolean;
}

export type WidgetButtonCustomStyles = Record<WidgetButtonState, WidgetButtonCustomStyle>;

export interface WidgetButtonAppearance {
  type: WidgetButtonType;
  autoScale: boolean;
  showLabel: boolean;
  label: string;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  borderRadius?: string;
  mainColor: string;
  backgroundColor: string;
  customStyle: WidgetButtonCustomStyles;
}

export const widgetButtonDefaultAppearance: WidgetButtonAppearance = {
  type: WidgetButtonType.outlined,
  autoScale: true,
  showLabel: true,
  label: 'Button',
  showIcon: true,
  icon: 'home',
  iconSize: 24,
  iconSizeUnit: 'px',
  mainColor: defaultMainColor,
  backgroundColor: defaultBackgroundColor,
  customStyle: {
    enabled: null,
    hovered: null,
    pressed: null,
    activated: null,
    disabled: null
  }
};

const mainColorVarPrefix = '--tb-widget-button-main-color-';
const backgroundColorVarPrefix = '--tb-widget-button-background-color-';
const boxShadowColorVarPrefix = '--tb-widget-button-box-shadow-color-';

abstract class ButtonStateCssGenerator {

  constructor() {}

  public generateStateCss(appearance: WidgetButtonAppearance): string {
    let mainColor = this.getMainColor(appearance);
    let backgroundColor = this.getBackgroundColor(appearance);
    const shadowEnabledByDefault = appearance.type !== WidgetButtonType.basic;
    let shadowColor = shadowEnabledByDefault ? defaultBoxShadowColor : defaultDisabledBoxShadowColor;
    const stateCustomStyle = appearance.customStyle[this.state];
    if (stateCustomStyle?.overrideMainColor && stateCustomStyle?.mainColor) {
      mainColor = stateCustomStyle.mainColor;
    }
    if (stateCustomStyle?.overrideBackgroundColor && stateCustomStyle?.backgroundColor) {
      backgroundColor = stateCustomStyle.backgroundColor;
    }
    if (stateCustomStyle?.overrideDropShadow) {
      shadowColor = !!stateCustomStyle.dropShadow ? defaultBoxShadowColor : defaultDisabledBoxShadowColor;
    }

    let css = `${mainColorVarPrefix}${this.state}: ${mainColor};\n`+
                     `${backgroundColorVarPrefix}${this.state}: ${backgroundColor};\n`+
                     `${boxShadowColorVarPrefix}${this.state}: ${shadowColor};`;
    const additionalCss = this.generateAdditionalStateCss(mainColor, backgroundColor);
    if (additionalCss) {
      css += `\n${additionalCss}`;
    }
    return css;
  }

  protected abstract get state(): WidgetButtonState;

  protected getMainColor(appearance: WidgetButtonAppearance): string {
    return appearance.mainColor || defaultMainColor;
  }

  protected getBackgroundColor(appearance: WidgetButtonAppearance): string {
    return appearance.backgroundColor || defaultBackgroundColor;
  }

  protected generateAdditionalStateCss(_mainColor: string, _backgroundColor: string): string {
    return null;
  }
}

class EnabledButtonStateCssGenerator extends ButtonStateCssGenerator {

  protected get state(): WidgetButtonState {
    return WidgetButtonState.enabled;
  }
}

class HoveredButtonStateCssGenerator extends ButtonStateCssGenerator {

  protected get state(): WidgetButtonState {
    return WidgetButtonState.hovered;
  }

  protected generateAdditionalStateCss(mainColor: string): string {
    const mainColorHoveredFilled = darkenColor(mainColor, hoveredFilledDarkenAmount);
    return `--tb-widget-button-main-color-hovered-filled: ${mainColorHoveredFilled};`;
  }
}

class PressedButtonStateCssGenerator extends ButtonStateCssGenerator {

  protected get state(): WidgetButtonState {
    return WidgetButtonState.pressed;
  }

  protected generateAdditionalStateCss(mainColor: string): string {
    const mainColorPressedFilled = darkenColor(mainColor, pressedFilledDarkenAmount);
    const mainColorInstance = tinycolor(mainColor);
    const mainColorPressedRipple = mainColorInstance.setAlpha(mainColorInstance.getAlpha() * 0.1).toRgbString();
    const mainColorPressedRippleFilled = darkenColor(mainColor, pressedRippleFilledDarkenAmount);
    return `--tb-widget-button-main-color-pressed-filled: ${mainColorPressedFilled};\n`+
           `--tb-widget-button-main-color-pressed-ripple: ${mainColorPressedRipple};\n`+
           `--tb-widget-button-main-color-pressed-ripple-filled: ${mainColorPressedRippleFilled};`;
  }
}

class ActivatedButtonStateCssGenerator extends ButtonStateCssGenerator {

  protected get state(): WidgetButtonState {
    return WidgetButtonState.activated;
  }

  protected generateAdditionalStateCss(mainColor: string): string {
    const mainColorActivatedFilled = darkenColor(mainColor, activatedFilledDarkenAmount);
    return `--tb-widget-button-main-color-activated-filled: ${mainColorActivatedFilled};`;
  }
}

class DisabledButtonStateCssGenerator extends ButtonStateCssGenerator {

  protected get state(): WidgetButtonState {
    return WidgetButtonState.disabled;
  }

  protected getMainColor(): string {
    return defaultMainColorDisabled;
  }

  protected getBackgroundColor(): string {
    return defaultBackgroundColorDisabled;
  }
}

const buttonStateCssGeneratorsMap = new Map<WidgetButtonState, ButtonStateCssGenerator>(
  [
    [WidgetButtonState.enabled, new EnabledButtonStateCssGenerator()],
    [WidgetButtonState.hovered, new HoveredButtonStateCssGenerator()],
    [WidgetButtonState.pressed, new PressedButtonStateCssGenerator()],
    [WidgetButtonState.activated, new ActivatedButtonStateCssGenerator()],
    [WidgetButtonState.disabled, new DisabledButtonStateCssGenerator()]
  ]
);

const widgetButtonCssSelector = '.mat-mdc-button.mat-mdc-button-base.tb-widget-button';

export const generateWidgetButtonAppearanceCss = (appearance: WidgetButtonAppearance): string => {
  let statesCss = '';
  for (const state of widgetButtonStates) {
    const generator = buttonStateCssGeneratorsMap.get(state);
    statesCss += `\n${generator.generateStateCss(appearance)}`;
  }
  return `${widgetButtonCssSelector} {\n`+
            `${statesCss}\n`+
    `}`;
};

const darkenColor = (inputColor: string, amount: number): string => {
  const input = tinycolor(inputColor);
  const brightness = input.getBrightness() / 255;
  let ratio: number;
  if (brightness >= 0.4 && brightness <= 0.5) {
    ratio = brightness + 0.2;
  } else {
    ratio = Math.max(0.1, Math.log10(brightness * 8));
  }
  return input.darken(ratio * amount).toRgbString();
};
