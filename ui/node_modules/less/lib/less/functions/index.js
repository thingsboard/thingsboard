import functionRegistry from './function-registry';
import functionCaller from './function-caller';

import boolean from './boolean';
import defaultFunc from './default';
import color from './color';
import colorBlending from './color-blending';
import dataUri from './data-uri';
import list from './list';
import math from './math';
import number from './number';
import string from './string';
import svg from './svg';
import types from './types';

export default environment => {
    const functions = { functionRegistry, functionCaller };

    // register functions
    functionRegistry.addMultiple(boolean);
    functionRegistry.add('default', defaultFunc.eval.bind(defaultFunc));
    functionRegistry.addMultiple(color);
    functionRegistry.addMultiple(colorBlending);
    functionRegistry.addMultiple(dataUri(environment));
    functionRegistry.addMultiple(list);
    functionRegistry.addMultiple(math);
    functionRegistry.addMultiple(number);
    functionRegistry.addMultiple(string);
    functionRegistry.addMultiple(svg(environment));
    functionRegistry.addMultiple(types);

    return functions;
};
