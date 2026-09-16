// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
type NestedKeyOf<ObjectType extends object> =
  {[Key in keyof ObjectType & (string | number)]: ObjectType[Key] extends object
    ? `${Key}` | `${Key}.${NestedKeyOf<ObjectType[Key]> extends infer U extends string ? U : never}`
    : `${Key}`
  }[keyof ObjectType & (string | number)];

type AllKeyOf<T> = T extends never ? never : keyof T;

type Optional<T, K> = { [P in Extract<keyof T, K>]?: T[P] };

type WithOptional<T, K extends AllKeyOf<T>> = T extends never ? never : Omit<T, K> & Optional<T, K>;
