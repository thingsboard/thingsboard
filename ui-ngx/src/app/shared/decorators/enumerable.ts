// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
export function enumerable(value: boolean) {
  return (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) => {
    descriptor.enumerable = value;
  };
}
