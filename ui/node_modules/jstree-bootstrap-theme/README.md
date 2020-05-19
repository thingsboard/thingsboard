jsTree Bootstrap Theme
=====================
Since there aren't many jsTree themes out there, we thought we'd make this one freely available. 
jsTree Bootstrap Theme is created as a part of [Proton UI Responsive Admin Panel Theme](http://proton.orangehilldev.com/) that we at [Orange Hill Development](http://www.orangehilldev.com) use to build custom admin panels. The theme is compatible with jsTree 3.

A legacy version compatible with jsTree pre 1.0 fix2 is still available on a [separate branch](https://github.com/orangehill/jstree-bootstrap-theme/tree/Legacy_jsTree_Bootstrap_Theme_(for_version_pre_1.0_fix2)) (no longer maintained).

## What is jsTree?
jsTree is a tree view for jQuery (depends on 1.9.1 or later). 
It is absolutely free (MIT licence) at [http://www.jstree.com/](http://www.jstree.com/) or at [https://github.com/vakata/jstree](https://github.com/vakata/jstree) and supports all modern browsers and IE from version 8 up. 
jsTree can display trees by parsing HTML or JSON and supports AJAX, it is themeable and easy to configure and customize. Events are fired when the user interacts with the tree. Other notable features are inline editing, drag'n'drop support, fuzzy searching (with optional server side calls), tri-state checkbox support, configurable node types, AMD compatibility, easily extendable via plugins.

## Theme Demo
Theme demo is available at [jsTree Bootstrap Theme Demo Page](http://orangehilldev.com/jstree-bootstrap-theme/demo/) .

## Responsiveness
jsTree Bootstrap Theme is [responsive](http://en.wikipedia.org/wiki/Responsive_web_design). To see the effect [open the demo](http://orangehilldev.com/jstree-bootstrap-theme/demo/) and scale a browser window down until the window width is less then 768 pixels. 

Mobile friendly design should make it easier to tap nodes with more precision.

## Getting Started
Download or checkout the latest copy and include jQuery and jsTree scripts as well as proton theme style file in your web page. Then create an instance (in this case using the inline HTML) with theme name set to proton and responsive (optional) set to true.

```html
<link rel="stylesheet" href="dist/themes/proton/style.min.css" />
<script src="dist/libs/jquery.js"></script>
<script src="dist/jstree.min.js"></script>
<div id="container">
  <ul>
    <li>Root node
      <ul>
        <li id="child_node">Child node</li>
      </ul>
    </li>
  </ul>
</div>
<script>
$(function() {
  $('#container').jstree({
    'core': {
        'themes': {
            'name': 'proton',
            'responsive': true
        }
    }
  });
});
</script>
```

## LESS support
If you wish to further customize the theme you might find it convenient to use included [LESS](http://lesscss.org/) files. The theme also includes a [grunt](https://github.com/gruntjs/grunt) script which you can use to build CSS files.

To develop using grunt files just run `grunt` (no options required). This will build theme images and CSS.

Do not edit files in the `dist` subdirectory as they are generated via grunt. You'll find theme source code in the `src/themes/proton` subdirectory.
