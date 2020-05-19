# 1.4.0 (2016-02-06)

## Features

- **dnd-handle directive**: This directive can be used in combination with `dnd-nodrag`, so that a `dnd-draggable` can only be dragged by using certain handle elements. [Demo](http://marceljuenemann.github.io/angular-drag-and-drop-lists/demo/#/types)
- **dnd-drop can handle insertion**: The `dnd-drop` callback can now return true to signalize that it will take care of inserting the dropped element itself. `dnd-list` will then no longer insert any elements into the list, but will still call the `dnd-inserted` callback.

## Bug Fixes

- **Fix dnd-disable-if on dnd-draggable**: When you disabled a `dnd-draggable` with `dnd-disable-if`, the user was still able to trigger a drag of that element by selecting some text inside the element. (issue #159)
- **dnd-list now handles the dragenter event**: According to the HTML5 standard dropzones need to handle the `dragenter` event, although there doesn't seem to be any browser that enforces this. (issue #118)

## Tested browsers

- Chrome 48 (Mac, Ubuntu & Windows 10)
- Firefox 44 (Ubuntu)
- Safari 9 (Mac)
- Microsoft Edge 20 (Windows 10)
- Internet Explorer 11 (Windows 10)
- Internet Explorer 10 & 9 in compatibility mode (Windows 10)

# 1.3.0 (2015-08-20)

## Features

- **New callbacks**: `dnd-dragend`, `dnd-canceled` and `dnd-inserted`.
- **Custom placeholder elements**: `dnd-list` elements can have custom elements by creating a child element with `dnd-placeholder` class. This is useful for cases where a simple `li` element is not sufficient.
- **dnd-nodrag directive**: This directive can be used inside `dnd-draggable` to prevent dragging certain areas. This is useful for input elements inside the draggable or creating handle elements.

## Bug Fixes

- **Fix user selection inside dnd-draggable**: The `selectstart` event is no longer cancelled.
- **Fix click handler compatibility**: Propagation of click events is now only stopped if the `dnd-selected` attribute is present.
- **Fix IE9 glitch**: Double clicks in IE9 previously would trigger the `dnd-moved` callback, and therefore remove items accidentially. (issue #21)

## Tested browsers

- Chrome 43 (Win7)
- Chrome 44 (Ubuntu)
- Chrome 44 (Mac)
- Firefox 40 (Win7)
- Firefox 39 (Ubuntu)
- Safari 8.0.8 (Mac)
- Internet Explorer 11 (IE9 & 10 in compatibility mode)

# 1.2.0 (2014-11-30)

## Bug Fixes

- **Fix glitches in Chrome**: When aborting a drag operation or dragging an element on itself, Chrome on Linux sometimes sends `move` as dropEffect instead of `none`. This lead to elements sometimes disappearing. Can be reproduced by dragging an element over itself and aborting with Esc key. (issue #14)
- **Fix dnd-allowed-types in nested lists**: When a drop was not allowed due to the wrong element type, the event was correctly propagated to the parent list. Nevertheless, the drop was still executed, because the drop handler didn't check the type again. (issue #16)

## Features

- **New callbacks**: The `dnd-draggable` directive now has a new `dnd-dragstart` callback besides the existing `dnd-moved` and `dnd-copied`. The `dnd-list` directive got the callbacks `dnd-dragover` and `dnd-drag` added, which are also able to abort a drop. (issue #11)
- **dnd-horizontal-list**: Lists can be marked as horizontal with this new attribute. The positioning algorithm then positions the placeholder left or right of other list items, instead of above or below. (issue #19)
- **dnd-external-sources**: This attribute allows drag and drop accross browser windows. See documentation for details. (issue #9)
- **pointer-events: none no longer required**: The dragover handler now traverses the DOM until it finds the list item node, therefore it's child elements no longer require the pointer-events: none style.

## Tested browsers

- Chrome 38 (Ubuntu)
- Chrome 38 (Win7)
- Chrome 39 (Mac)
- Firefox 31 (Win7)
- Firefox 33 (Ubuntu)
- Safari 7.1 (Mac)
- Internet Explorer 11 (IE9 & 10 in compatibility mode)

# 1.1.0 (2014-08-31)

## Bug Fixes

- **jQuery compatibility**: jQuery wraps browser events in event.originalEvent

## Features

- **dnd-disable-if attribute**: allows to dynamically disable the drag and drop functionality
- **dnd-type and dnd-allowed-types**: allows to restrict an item to specifc lists depending on it's type

## Tested browsers

- Chrome 34 (Ubuntu)
- Chrome 37 (Mac)
- Chrome 37 (Win7)
- Firefox 28 (Win7)
- Firefox 31 (Ubuntu)
- Safari 7.0.6 (Mac)
- Internet Explorer 11 (IE9 & 10 in compatibility mode)

# 1.0.0 (2014-04-11)

Initial release

# Release checklist

- Bump versions
  - bower.json
  - package.json
  - JS files
- Minify and test (npm run-script minify)
- Test different OS & browsers (npm start)
- Update README and CHANGELOG
- Merge to master
- Tag release
- Merge to gh-pages
- Publish to npm
