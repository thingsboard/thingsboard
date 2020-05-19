export declare type ElementClass = Function;
export declare type Property = PropertyDescriptor | Function;
export declare function spyElementPrototypes<T extends ElementClass>(elementClass: T, properties: Record<string, Property>): {
    mockRestore(): void;
};
export declare function spyElementPrototype(Element: ElementClass, propName: string, property: Property): {
    mockRestore(): void;
};
