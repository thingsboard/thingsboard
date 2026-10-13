// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { ImagePipe } from '@shared/pipe/image.pipe';

@Injectable({
  providedIn: 'root'
})
export class FaviconService {

  private favicon = $('link[rel="icon"]');

  constructor(
    private whiteLabelingService: WhiteLabelingService,
    private imagePipe: ImagePipe
  ) {}

  setFavicon() {
    const url = this.whiteLabelingService.faviconUrl();
    const loginFavicon = !this.whiteLabelingService.isUserWlMode;
    this.imagePipe.transform(url, {asString: true, ignoreLoadingImage: true, loginFavicon}).subscribe(
      (faviconUrl) => {
        this.favicon.attr('href', faviconUrl as string);
      }
    );
  }
}
