// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { CustomerService } from '@core/http/customer.service';
import { Observable, of } from 'rxjs';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { map } from 'rxjs/operators';

@Injectable()
export class CustomerTitleResolver  {

  constructor(private customerService: CustomerService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<string> {
    const params = resolveGroupParams(route);
    if (params.customerId) {
      return this.customerService.getShortCustomerInfo(params.customerId).pipe(
        map((info) => info.title)
      );
    } else {
      return of(null);
    }
  }
}
