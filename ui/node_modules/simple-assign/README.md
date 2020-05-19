## simple-assign

This is a simple implementation `Object.assign` that has been graciously stolen from [babel's extends helper](https://github.com/babel/babel/blob/v6.4.6/packages/babel-helpers/src/helpers.js#L165-L177).

**Note:** This is just the function implementation. It is **not** a ponyfill and it  **does not** check for the existence of `Object.assign` for you. To make a true [ponyfill](https://kikobeats.com/polyfill-ponyfill-and-prollyfill/) that returns the native implementation if available, you can do something like this:

**Example:**
```js
import assign from 'simple-assign';

export default Object.assign || assign;
```
