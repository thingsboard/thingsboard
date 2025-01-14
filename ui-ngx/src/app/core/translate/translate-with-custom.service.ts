import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { isString } from '@core/utils';
import { customTranslationsPrefix, i18nPrefix } from '@shared/models/constants';

@Injectable()
export class TranslateWithCustomService extends TranslateService {

  private readonly i18nRegExp = new RegExp(`{${i18nPrefix}:[^{}]+}`, 'g');
  private readonly prefix = `{${i18nPrefix}`;

  instant(key: string, interpolateParams: Object = {}): string | any {
    if (key.includes(this.prefix)) {
      return this.customTranslation(key);
    } else {
      interpolateParams = this.customTranslateInterpolateParams(interpolateParams);
      return super.instant(key, interpolateParams);
    }
  }

  public customTranslation(translationValue: string, defaultValue?: string): string {
    if (!defaultValue) {
      defaultValue = translationValue;
    }
    if (translationValue && isString(translationValue)) {
      if (translationValue.includes(this.prefix)) {
        const matches = translationValue.match(this.i18nRegExp);
        let result = translationValue;
        for (const match of matches) {
          const translationId = match.substring(6, match.length - 1);
          result = result.replace(match, this.doTranslate(translationId, match));
        }
        return result;
      } else {
        return this.doTranslate(translationValue, defaultValue, customTranslationsPrefix);
      }
    } else {
      return translationValue;
    }
  }

  private customTranslateInterpolateParams(interpolateParams: Object = {}): object {
    const interpolateParamsValues = Object.values(interpolateParams);
    if (interpolateParamsValues.length && interpolateParamsValues.some(value => value && isString(value) && value.includes(`{${i18nPrefix}`))) {
      interpolateParams = Object.keys(interpolateParams).reduce((acc, key) => {
        return {...acc, [key]: this.customTranslation(interpolateParams[key])};
      }, {})
    }
    return interpolateParams;
  }

  private doTranslate(translationValue: string, defaultValue: string, prefix?: string): string {
    let result: string;
    let translationId;
    if (prefix) {
      translationId = prefix + translationValue;
    } else {
      translationId = translationValue;
    }
    const translation = super.instant(translationId);
    if (translation !== translationId) {
      result = translation + '';
    } else {
      result = defaultValue;
    }
    return result;
  }
}
