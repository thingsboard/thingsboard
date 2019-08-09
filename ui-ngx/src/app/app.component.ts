///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, OnInit } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from './core/core.state';
import { LocalStorageService } from './core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material';

@Component({
  selector: 'tb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer) {

    console.log(`ThingsBoard Version: ${env.tbVersion}`);

    this.matIconRegistry.addSvgIconSetInNamespace('mdi',
      this.domSanitizer.bypassSecurityTrustResourceUrl('./assets/mdi.svg'));

    this.storageService.testLocalStorage();

    this.setupTranslate();
  }

  setupTranslate() {
    console.log(`Supported Langs: ${env.supportedLangs}`);
    this.translate.addLangs(env.supportedLangs);
    console.log(`Default Lang: ${env.defaultLang}`);
    this.translate.setDefaultLang(env.defaultLang);
  }

  ngOnInit() {
  }

}

