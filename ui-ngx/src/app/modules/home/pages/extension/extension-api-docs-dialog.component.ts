// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ExtensionService } from '@core/http/extension.service';
import { ResourcesService } from '@core/services/resources.service';
import { AuthService } from '@core/auth/auth.service';
import { Extension, extensionRouteBasePath, extensionUrlPath } from '@shared/models/extension.models';
import { PageLink } from '@shared/models/page/page-link';
import { forkJoin, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface ExtensionApiDocsDialogData {
  extensionId: string;
  extensionName: string;
}

// The Swagger UI bundle is not an npm dependency: it is loaded at runtime from the springdoc webjar
// tb-core already serves version-less under /swagger-ui/*. One source keeps the renderer in lockstep
// with the platform's own API docs page and off this app's bundle.
declare const SwaggerUIBundle: any;

@Component({
  selector: 'tb-extension-api-docs-dialog',
  templateUrl: './extension-api-docs-dialog.component.html',
  styleUrls: ['./extension-api-docs-dialog.component.scss'],
  standalone: false
})
export class ExtensionApiDocsDialogComponent extends
  DialogComponent<ExtensionApiDocsDialogComponent, void> implements AfterViewInit, OnDestroy {

  @ViewChild('swaggerContainer', {static: true}) swaggerContainer: ElementRef<HTMLElement>;

  extensions: Array<Extension> = [];
  selectedExtensionId: string;
  loading = false;
  error: string = null;
  // monotonic counter of spec requests; only the newest one may render (see loadSpec)
  private specRequestSeq = 0;
  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private extensionService: ExtensionService,
              private resources: ResourcesService,
              @Inject(MAT_DIALOG_DATA) public data: ExtensionApiDocsDialogData,
              public dialogRef: MatDialogRef<ExtensionApiDocsDialogComponent, void>,
              public translate: TranslateService) {
    super(store, router, dialogRef);
    this.selectedExtensionId = data.extensionId;
  }

  ngAfterViewInit(): void {
    this.loading = true;
    forkJoin([
      this.resources.loadResource('/swagger-ui/swagger-ui.css'),
      this.resources.loadResource('/swagger-ui/swagger-ui-bundle.js')
    ]).subscribe({
      next: () => this.loadSpec(),
      error: () => {
        this.loading = false;
        this.error = this.translate.instant('extension.api-docs-renderer-not-available');
      }
    });
    // The dropdown is best effort: if the list fails, the dialog still renders the extension it was
    // opened for.
    this.extensionService.getExtensions(new PageLink(100), {ignoreErrors: true}).subscribe({
      next: (page) => this.extensions = page.data,
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.swaggerContainer.nativeElement.innerHTML = '';
    super.ngOnDestroy();
  }

  loadSpec(): void {
    // The selector stays enabled while a spec loads, so two requests can be in flight at once and
    // answer out of order. Each request is numbered; only the newest may render or report an error,
    // so an earlier extension's spec (and its "Try it out" route) can never appear under a later
    // selection. The subscription is also released with the dialog.
    const requestedId = this.selectedExtensionId;
    const seq = ++this.specRequestSeq;
    this.loading = true;
    this.error = null;
    this.swaggerContainer.nativeElement.innerHTML = '';
    this.extensionService.getExtensionApiDocs(requestedId, {ignoreErrors: true}).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (spec) => {
        if (seq !== this.specRequestSeq) {
          return;
        }
        this.loading = false;
        this.renderSpec(spec, this.routeOf(requestedId));
      },
      error: (err) => {
        if (seq !== this.specRequestSeq) {
          return;
        }
        this.loading = false;
        this.error = err?.error?.message || this.translate.instant('extension.api-docs-not-available');
      }
    });
  }

  // The canonical data-plane route of the selected extension, as the backend pins it into `servers`.
  private routeOf(extensionId: string): string {
    const extension = this.extensions.find(e => e.id.id === extensionId);
    return extension ? (extensionUrlPath(extension) || null) : null;
  }

  private renderSpec(spec: object, route: string): void {
    SwaggerUIBundle({
      spec,
      domNode: this.swaggerContainer.nativeElement,
      presets: [SwaggerUIBundle.presets.apis],
      layout: 'BaseLayout',
      // "Try it out" targets the relative data-plane path the backend pinned into `servers`. The
      // platform credential is attached only to a request that is same-origin AND resolves under this
      // extension's route: the backend strips path/operation-level server overrides too, but the final
      // URL is what a hostile document ultimately controls, so it is checked here once more before
      // anything sensitive is added. Anything else is sent bare.
      requestInterceptor: (req: any) => {
        if (req?.url && this.withinRoute(req.url, route)) {
          req.headers = req.headers || {};
          req.headers['X-Authorization'] = `Bearer ${AuthService.getJwtToken()}`;
        }
        return req;
      }
    });
  }

  private withinRoute(url: string, route: string): boolean {
    let resolved: URL;
    try {
      resolved = new URL(url, window.location.origin);
    } catch (e) {
      return false;
    }
    if (resolved.origin !== window.location.origin) {
      return false;
    }
    if (!route) {
      // The extension list did not load, so the slug is unknown: fall back to the route prefix, which
      // still keeps the credential off every platform API path.
      return resolved.pathname === extensionRouteBasePath || resolved.pathname.startsWith(extensionRouteBasePath + '/');
    }
    return resolved.pathname === route || resolved.pathname.startsWith(route + '/');
  }

  close() {
    this.dialogRef.close();
  }
}
