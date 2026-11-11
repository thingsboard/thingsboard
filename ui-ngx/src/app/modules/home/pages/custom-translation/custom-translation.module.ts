// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { TranslationTableComponent } from '@home/pages/custom-translation/translation-table.component';
import { CustomTranslationRoutingModule } from '@home/pages/custom-translation/custom-translation-routing.module';
import { AddNewLanguageDialogComponent } from './add-new-language-dialog.component';
import { LanguageAutocompleteComponent } from './language-autocomplete.component';
import { CustomTranslationComponent } from './custom-translation.component';
import { TranslationMapTableComponent } from './translation-map-table.component';
import { TranslationMapAdvancedComponent } from '@home/pages/custom-translation/translation-map-advanced.component';

@NgModule({
  declarations: [
    TranslationTableComponent,
    AddNewLanguageDialogComponent,
    LanguageAutocompleteComponent,
    CustomTranslationComponent,
    TranslationMapTableComponent,
    TranslationMapAdvancedComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CustomTranslationRoutingModule
  ]
})
export class CustomTranslationModule { }
