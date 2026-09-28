// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { LoginResponse } from '@shared/models/login.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { first } from 'rxjs/operators';

@Component({
    selector: 'tb-email-verified',
    templateUrl: './email-verified.component.html',
    styleUrls: ['./email-verification.component.scss'],
    standalone: false
})
export class EmailVerifiedComponent {

  activated: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  isLoading = true;

  @HostBinding('class') class = 'tb-custom-css';

  private emailCode = '';
  private loginResponse: LoginResponse;

  constructor(private route: ActivatedRoute,
              private router: Router,
              public wl: WhiteLabelingService,
              private authService: AuthService) {
    this.route.queryParams
      .pipe(
        first()
      ).subscribe(params => {
        this.emailCode = decodeURIComponent(params.emailCode || '');
        this.activateAndGetCredentials();
      }
    );
  }

  login(): void {
    if (this.loginResponse) {
      this.isLoading = true;
      this.authService.setUserFromJwtToken(this.loginResponse.token, this.loginResponse.refreshToken, true).subscribe({
        next: (value) => {this.isLoading = value;},
        error: () => {this.isLoading = false;}
      });
    } else {
      this.router.navigateByUrl(`/login`).then(() => {});
    }
  }

  private activateAndGetCredentials(): void {
    this.authService.activateByEmailCode(this.emailCode).subscribe({
      next: (loginResponse) => {
        this.loginResponse = loginResponse;
        this.activated.next(true);
        this.isLoading = false;
      },
      error: () => {this.isLoading = false;}
    });
  }

}
