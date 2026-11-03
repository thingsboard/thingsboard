// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  ComponentRef,
  Directive, EventEmitter, Injector,
  Input,
  OnChanges, Output, Renderer2,
  SimpleChange,
  SimpleChanges,
  Type,
  ViewContainerRef
} from '@angular/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[tbComponentOutlet]',
    exportAs: 'tbComponentOutlet',
    standalone: false
})
export class TbComponentOutletDirective<_T = unknown> implements OnChanges {
  private componentRef: ComponentRef<any> | null = null;
  private context = new TbComponentOutletContext();
  @Input() tbComponentOutletContext: any | null = null;
  @Input() tbComponentStyle: { [klass: string]: any } | null = null;
  @Input() tbComponentInjector: Injector | null = null;
  @Input() tbComponentOutlet: Type<any> = null;
  @Output() componentChange = new EventEmitter<ComponentRef<any>>();

  static ngTemplateContextGuard<T>(
    _dir: TbComponentOutletDirective<T>,
    _ctx: any
  ): _ctx is TbComponentOutletContext {
    return true;
  }

  private recreateComponent(): void {
    this.viewContainer.clear();
    this.componentRef = this.viewContainer.createComponent(this.tbComponentOutlet, {index: 0, injector: this.tbComponentInjector});
    this.componentChange.next(this.componentRef);
    if (this.tbComponentOutletContext) {
      for (const propName of Object.keys(this.tbComponentOutletContext)) {
        this.componentRef.instance[propName] = this.tbComponentOutletContext[propName];
      }
    }
    if (this.tbComponentStyle) {
      for (const propName of Object.keys(this.tbComponentStyle)) {
        this.renderer.setStyle(this.componentRef.location.nativeElement, propName, this.tbComponentStyle[propName]);
      }
    }
  }

  private updateContext(): void {
    const newCtx = this.tbComponentOutletContext;
    const oldCtx = this.componentRef.instance as any;
    if (newCtx) {
      for (const propName of Object.keys(newCtx)) {
        oldCtx[propName] = newCtx[propName];
      }
    }
  }

  constructor(private viewContainer: ViewContainerRef,
              private renderer: Renderer2) {}

  ngOnChanges(changes: SimpleChanges): void {
    const { tbComponentOutletContext, tbComponentOutlet } = changes;
    const shouldRecreateComponent = (): boolean => {
      let shouldOutletRecreate = false;
      if (tbComponentOutlet) {
        if (tbComponentOutlet.firstChange) {
          shouldOutletRecreate = true;
        } else {
          const isPreviousOutletTemplate = tbComponentOutlet.previousValue instanceof Type;
          const isCurrentOutletTemplate = tbComponentOutlet.currentValue instanceof Type;
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
        tbComponentOutletContext && hasContextShapeChanged(tbComponentOutletContext);
      return shouldContextRecreate || shouldOutletRecreate;
    };

    if (tbComponentOutlet) {
      this.context.$implicit = tbComponentOutlet.currentValue;
    }

    const recreateComponent = shouldRecreateComponent();
    if (recreateComponent) {
      this.recreateComponent();
    } else {
      this.updateContext();
    }
  }
}

export class TbComponentOutletContext {
  public $implicit: any;
}
