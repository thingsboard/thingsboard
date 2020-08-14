export function enumerable(value: boolean) {
  return (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) => {
    descriptor.enumerable = value;
  };
}
