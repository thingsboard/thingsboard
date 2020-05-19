export default function useControlledState<T, R = T>(defaultStateValue: T | (() => T), option?: {
    defaultValue?: T | (() => T);
    value?: T;
    onChange?: (value: T, prevValue: T) => void;
    postState?: (value: T) => T;
}): [R, (value: T) => void];
