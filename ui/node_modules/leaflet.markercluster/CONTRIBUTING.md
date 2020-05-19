Contributing to Leaflet.MarkerCluster
=====================================

 1. [Reporting Bugs](#reporting-bugs)
 2. [Contributing Code](#contributing-code)
 3. [Building](#building)
 4. [Testing](#testing)

## Reporting Bugs

Before reporting a bug on the project's [issues page](https://github.com/Leaflet/Leaflet.markercluster/issues),
first make sure that your issue is caused by Leaflet.MarkerCluster, not your application code
(e.g. passing incorrect arguments to methods, etc.).
Second, search the already reported issues for similar cases,
and if it's already reported, just add any additional details in the comments.

After you've made sure that you've found a new Leaflet.markercluster bug,
here are some tips for creating a helpful report that will make fixing it much easier and quicker:

 * Write a **descriptive, specific title**. Bad: *Problem with polylines*. Good: *Doing X in IE9 causes Z*.
 * Include **browser, OS and Leaflet version** info in the description.
 * Create a **simple test case** that demonstrates the bug (e.g. using [JSFiddle](http://jsfiddle.net/) or [JS Bin](http://jsbin.com/)).
 * Check whether the bug can be reproduced in **other browsers**.
 * Check if the bug occurs in the stable version, master, or both.
 * *Bonus tip:* if the bug only appears in the master version but the stable version is fine,
   use `git bisect` to find the exact commit that introduced the bug.

If you just want some help with your project,
try asking [on the Leaflet forum](https://groups.google.com/forum/#!forum/leaflet-js) instead.

## Contributing Code

### Considerations for Accepting Patches

While we happily accept patches, we're also committed to keeping Leaflet simple, lightweight and blazingly fast.
So bugfixes, performance optimizations and small improvements that don't add a lot of code
are much more likely to get accepted quickly.

Before sending a pull request with a new feature, check if it's been discussed before already
(either on [GitHub issues](https://github.com/Leaflet/Leaflet/issues)
or [Leaflet UserVoice](http://leaflet.uservoice.com/)),
and ask yourself two questions:

 1. Are you sure that this new feature is important enough to justify its presence in the Leaflet core?
    Or will it look better as a plugin in a separate repository?
 2. Is it written in a simple, concise way that doesn't add bulk to the codebase?

If your feature or API improvement did get merged into master,
please consider submitting another pull request with the corresponding [documentation update](#improving-documentation).

## Building

Install the dependencies:
```
npm install -g jake
npm install
```

Then to build:
```
jake
```
Output will be in the ```dist/``` directory

## Testing

To run unit tests:
```
jake test
```
