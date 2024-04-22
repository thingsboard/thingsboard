import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

export class TranslateDefaultLoader implements TranslateLoader {

  constructor(private http: HttpClient) {

  }

  getTranslation(lang: string): Observable<object> {
    return this.http.get(`/assets/locale/locale.constant-${lang}.json`);
  }
}
