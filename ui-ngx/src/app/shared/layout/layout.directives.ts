///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Directive } from '@angular/core';
import { BREAKPOINT, LayoutAlignDirective, LayoutDirective, LayoutGapDirective, ShowHideDirective } from '@angular/flex-layout';

const TB_BREAKPOINTS = [
  {
    alias: 'md-lg',
    mediaQuery: 'screen and (min-width: 960px) and (max-width: 1819px)',
    priority: 750
  },
  {
    alias: 'gt-md-lg',
    mediaQuery: 'screen and (min-width: 1820px)',
    priority: -600
  }
];

export const TbBreakPointsProvider = {
  provide: BREAKPOINT,
  useValue: TB_BREAKPOINTS,
  multi: true
};

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayout.md-lg]', inputs: ['fxLayout.md-lg']})
export class MdLgLayoutDirective extends LayoutDirective {
  protected inputs = ['fxLayout.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutAlign.md-lg]', inputs: ['fxLayoutAlign.md-lg']})
export class MdLgLayoutAlignDirective extends LayoutAlignDirective {
  protected inputs = ['fxLayoutAlign.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutGap.md-lg]', inputs: ['fxLayoutGap.md-lg']})
export class MdLgLayoutGapDirective extends LayoutGapDirective {
  protected inputs = ['fxLayoutGap.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxHide.md-lg]', inputs: ['fxHide.md-lg']})
export class MdLgShowHideDirective extends ShowHideDirective {
  protected inputs = ['fxHide.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayout.gt-md-lg]', inputs: ['fxLayout.gt-md-lg']})
export class GtMdLgLayoutDirective extends LayoutDirective {
  protected inputs = ['fxLayout.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutAlign.gt-md-lg]', inputs: ['fxLayoutAlign.gt-md-lg']})
export class GtMdLgLayoutAlignDirective extends LayoutAlignDirective {
  protected inputs = ['fxLayoutAlign.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutGap.gt-md-lg]', inputs: ['fxLayoutGap.gt-md-lg']})
export class GtMdLgLayoutGapDirective extends LayoutGapDirective {
  protected inputs = ['fxLayoutGap.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxHide.gt-md-lg]', inputs: ['fxHide.gt-md-lg']})
export class GtMdLgShowHideDirective extends ShowHideDirective {
  protected inputs = ['fxHide.gt-md-lg'];
}
