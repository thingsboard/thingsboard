// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute } from '@angular/router';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { first } from 'rxjs/operators';

@Component({
    selector: 'tb-email-verification',
    templateUrl: './email-verification.component.html',
    styleUrls: ['./email-verification.component.scss'],
    standalone: false
})
export class EmailVerificationComponent extends PageComponent {

  @HostBinding('class') class = 'tb-custom-css';

  private email = '';

  constructor(private route: ActivatedRoute,
              public wl: WhiteLabelingService,
              private authService: AuthService) {
    super();
    this.route.queryParams
      .pipe(
        first()
      ).subscribe(params => {
        this.email = decodeURIComponent(params.email || '');
      }
    );
  }

  resendEmail(): void {
    this.authService.resendEmailActivation(this.email).subscribe(() => {});
  }
}
