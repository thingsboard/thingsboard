// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  coerceArray as coerceArrayAngular,
  coerceBooleanProperty,
  coerceCssPixelValue as coerceCssPixelValueAngular,
  coerceNumberProperty,
  coerceStringArray as coerceStringArrayAngular
} from '@angular/cdk/coercion';

export const coerceBoolean = () => (target: any, key: string, propertyDescriptor?: PropertyDescriptor): void => {
  if (!!propertyDescriptor && !!propertyDescriptor.set) {
    const original = propertyDescriptor.set;

    propertyDescriptor.set = function(next) {
      original.apply(this, [coerceBooleanProperty(next)]);
    };
  } else {
    const getter = function() {
      return this['__' + key];
    };

    const setter = function(next: any) {
      this['__' + key] = coerceBooleanProperty(next);
    };

    Object.defineProperty(target, key, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    });
  }
};

export const coerceNumber = () => (target: any, key: string, propertyDescriptor?: PropertyDescriptor): void => {
  if (!!propertyDescriptor && !!propertyDescriptor.set) {
    const original = propertyDescriptor.set;

    propertyDescriptor.set = function(next) {
      original.apply(this, [coerceNumberProperty(next)]);
    };
  } else {
    const getter = function() {
      return this['__' + key];
    };

    const setter = function(next: any) {
      this['__' + key] = coerceNumberProperty(next);
    };

    Object.defineProperty(target, key, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    });
  }
};

export const coerceCssPixelValue = () => (target: any, key: string, propertyDescriptor?: PropertyDescriptor): void => {
  if (!!propertyDescriptor && !!propertyDescriptor.set) {
    const original = propertyDescriptor.set;

    propertyDescriptor.set = function(next) {
      original.apply(this, [coerceCssPixelValueAngular(next)]);
    };
  } else {
    const getter = function() {
      return this['__' + key];
    };

    const setter = function(next: any) {
      this['__' + key] = coerceCssPixelValueAngular(next);
    };

    Object.defineProperty(target, key, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    });
  }
};

export const coerceArray = () => (target: any, key: string, propertyDescriptor?: PropertyDescriptor): void => {
  if (!!propertyDescriptor && !!propertyDescriptor.set) {
    const original = propertyDescriptor.set;

    propertyDescriptor.set = function(next) {
      original.apply(this, [coerceArrayAngular(next)]);
    };
  } else {
    const getter = function() {
      return this['__' + key];
    };

    const setter = function(next: any) {
      this['__' + key] = coerceArrayAngular(next);
    };

    Object.defineProperty(target, key, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    });
  }
};

export const coerceStringArray = (separator?: string | RegExp) =>
  (target: any, key: string, propertyDescriptor?: PropertyDescriptor): void => {
  if (!!propertyDescriptor && !!propertyDescriptor.set) {
    const original = propertyDescriptor.set;

    propertyDescriptor.set = function(next) {
      original.apply(this, [coerceStringArrayAngular(next, separator)]);
    };
  } else {
    const getter = function() {
      return this['__' + key];
    };

    const setter = function(next: any) {
      this['__' + key] = coerceStringArrayAngular(next, separator);
    };

    Object.defineProperty(target, key, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    });
  }
};
