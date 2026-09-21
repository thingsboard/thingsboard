// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule, IMAGE_CONFIG } from '@angular/common';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { GlobalHttpInterceptor } from './interceptors/global-http-interceptor';
import { effects, metaReducers, reducers } from './core.state';
import { environment as env } from '@env/environment';

import {
  MissingTranslationHandler,
  TranslateCompiler,
  TranslateLoader,
  TranslateModule,
  TranslateParser
} from '@ngx-translate/core';
import { TbMissingTranslationHandler } from './translate/missing-translate-handler';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DEFAULT_OPTIONS, MatDialogConfig, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateDefaultCompiler } from '@core/translate/translate-default-compiler';
import { WINDOW_PROVIDERS } from '@core/services/window.service';
import { HotkeyModule } from 'angular2-hotkeys';
import { TranslateDefaultParser } from '@core/translate/translate-default-parser';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';
import { EntityConflictInterceptor } from '@core/interceptors/entity-conflict.interceptor';

@NgModule({ exports: [], imports: [CommonModule,
        MatDialogModule,
        MatButtonModule,
        MatSnackBarModule,
        // ngx-translate
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useClass: TranslateDefaultLoader
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: TbMissingTranslationHandler
            },
            compiler: {
                provide: TranslateCompiler,
                useClass: TranslateDefaultCompiler
            },
            parser: {
                provide: TranslateParser,
                useClass: TranslateDefaultParser
            }
        }),
        HotkeyModule.forRoot(),
        // ngrx
        StoreModule.forRoot(reducers, { metaReducers,
            runtimeChecks: {
                strictStateImmutability: true,
                strictActionImmutability: true,
                strictStateSerializability: true,
                strictActionSerializability: true
            } }),
        EffectsModule.forRoot(effects),
        env.production
            ? []
            : StoreDevtoolsModule.instrument({
                name: env.appTitle,
                connectInZone: true
            })], providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: GlobalHttpInterceptor,
            multi: true
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: EntityConflictInterceptor,
            multi: true
        },
        {
            provide: MAT_DIALOG_DEFAULT_OPTIONS,
            useValue: {
                ...new MatDialogConfig(),
                restoreFocus: false
            }
        },
        WINDOW_PROVIDERS,
        provideHttpClient(withInterceptorsFromDi()),
       {
            provide: IMAGE_CONFIG,
            useValue: {
              disableImageSizeWarning: true,
              disableImageLazyLoadWarning: true
            }
       }
    ] })
export class CoreModule {
}
