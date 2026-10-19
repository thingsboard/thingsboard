// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { CoreModule } from '@core/core.module';
import { LoginModule } from '@modules/login/login.module';
import { HomeModule } from '@home/home.module';

import { AppComponent } from './app.component';
import { DashboardRoutingModule } from '@modules/dashboard/dashboard-routing.module';
import { RouterModule, Routes } from '@angular/router';

import { DefaultUrlSerializer, UrlSerializer, UrlTree } from '@angular/router';

export default class TbUrlSerializer implements UrlSerializer {
  private _defaultUrlSerializer: DefaultUrlSerializer = new DefaultUrlSerializer();

  parse(url: string): UrlTree {
    // Encode parentheses
    url = url.replace(/\(/g, '%28').replace(/\)/g, '%29');
    // Use the default serializer.
    return this._defaultUrlSerializer.parse(url)
  }

  serialize(tree: UrlTree): string {
    return this._defaultUrlSerializer.serialize(tree).replace(/%28/g, '(').replace(/%29/g, ')');
  }
}

const routes: Routes = [
  { path: '**',
    redirectTo: 'home'
  }
];

@NgModule({
  imports: [
    RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PageNotFoundRoutingModule { }


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    CoreModule,
    LoginModule,
    HomeModule,
    DashboardRoutingModule,
    PageNotFoundRoutingModule
  ],
  providers: [
    { provide: UrlSerializer, useClass: TbUrlSerializer }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
