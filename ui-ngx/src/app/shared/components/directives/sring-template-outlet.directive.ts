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

import {
  Directive,
  EmbeddedViewRef,
  Input,
  OnChanges,
  SimpleChange,
  SimpleChanges,
  TemplateRef,
  ViewContainerRef
} from '@angular/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[tbStringTemplateOutlet]',
    exportAs: 'tbStringTemplateOutlet',
    standalone: false
})
export class TbStringTemplateOutletDirective<_T = unknown> implements OnChanges {
  private embeddedViewRef: EmbeddedViewRef<any> | null = null;
  private context = new TbStringTemplateOutletContext();
  @Input() tbStringTemplateOutletContext: any | null = null;
  @Input() tbStringTemplateOutlet: any | TemplateRef<any> = null;

  static ngTemplateContextGuard<T>(
    // eslint-disable-next-line @typescript-eslint/naming-convention,no-underscore-dangle,id-blacklist,id-match
    _dir: TbStringTemplateOutletDirective<T>,
    // eslint-disable-next-line @typescript-eslint/naming-convention, no-underscore-dangle, id-blacklist, id-match
    _ctx: any
  ): _ctx is TbStringTemplateOutletContext {
    return true;
  }

  private recreateView(): void {
    this.viewContainer.clear();
    const isTemplateRef = this.tbStringTemplateOutlet instanceof TemplateRef;
    const templateRef = (isTemplateRef ? this.tbStringTemplateOutlet : this.templateRef) as any;
    this.embeddedViewRef = this.viewContainer.createEmbeddedView(
      templateRef,
      isTemplateRef ? this.tbStringTemplateOutletContext : this.context
    );
  }

  private updateContext(): void {
    const isTemplateRef = this.tbStringTemplateOutlet instanceof TemplateRef;
    const newCtx = isTemplateRef ? this.tbStringTemplateOutletContext : this.context;
    const oldCtx = this.embeddedViewRef.context as any;
    if (newCtx) {
      for (const propName of Object.keys(newCtx)) {
        oldCtx[propName] = newCtx[propName];
      }
    }
  }

  constructor(private viewContainer: ViewContainerRef, private templateRef: TemplateRef<any>) {}

  ngOnChanges(changes: SimpleChanges): void {
    const { tbStringTemplateOutletContext, tbStringTemplateOutlet } = changes;
    const shouldRecreateView = (): boolean => {
      let shouldOutletRecreate = false;
      if (tbStringTemplateOutlet) {
        if (tbStringTemplateOutlet.firstChange) {
          shouldOutletRecreate = true;
        } else {
          const isPreviousOutletTemplate = tbStringTemplateOutlet.previousValue instanceof TemplateRef;
          const isCurrentOutletTemplate = tbStringTemplateOutlet.currentValue instanceof TemplateRef;
          shouldOutletRecreate = isPreviousOutletTemplate || isCurrentOutletTemplate;
        }
      }
      const hasContextShapeChanged = (ctxChange: SimpleChange): boolean => {
        const prevCtxKeys = Object.keys(ctxChange.previousValue || {});
        const currCtxKeys = Object.keys(ctxChange.currentValue || {});
        if (prevCtxKeys.length === currCtxKeys.length) {
          for (const propName of currCtxKeys) {
            if (prevCtxKeys.indexOf(propName) === -1) {
              return true;
            }
          }
          return false;
        } else {
          return true;
        }
      };
      const shouldContextRecreate =
        tbStringTemplateOutletContext && hasContextShapeChanged(tbStringTemplateOutletContext);
      return shouldContextRecreate || shouldOutletRecreate;
    };

    if (tbStringTemplateOutlet) {
      this.context.$implicit = tbStringTemplateOutlet.currentValue;
    }

    const recreateView = shouldRecreateView();
    if (recreateView) {
      this.recreateView();
    } else {
      this.updateContext();
    }
  }
}

export class TbStringTemplateOutletContext {
  public $implicit: any;
}
