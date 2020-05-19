Tooltipster
===========

A flexible and extensible jQuery plugin for modern tooltips by Caleb Jacob and Louis Ameline under MIT license.  
Compatible with Mozilla Firefox, Google Chrome, IE6+ and others.  
Requires jQuery 1.10+ (or less, see the compatibility note in the doc).  
Default css + js files = 10Kb gzipped.

A reminder of options/methods lies below. For detailed documentation, visit http://iamceege.github.io/tooltipster/

Standard options
----------------

animation  
animationDuration  
content  
contentAsHTML  
contentCloning  
debug  
delay  
delayTouch  
functionInit  
functionBefore  
functionReady  
functionAfter  
functionFormat  
IEmin  
interactive  
multiple  
plugins  
repositionOnScroll  
restoration  
selfDestruction  
timer  
theme  
trackerInterval  
trackOrigin  
trackTooltip  
trigger  
triggerClose  
triggerOpen  
updateAnimation  
zIndex  

Other options
-------------

(these are available when you use sideTip, the default plugin)

arrow  
distance  
functionPosition  
maxWidth  
minIntersection  
minWidth  
side  
viewportAware  

Instance methods
----------------

close([callback])  
content([myNewContent])  
destroy()  
disable()  
elementOrigin()  
elementTooltip()  
enable()  
instance()  
on, one, off, triggerHandler  
open([callback])  
option(optionName [, optionValue])  
reposition()   
status()   

Core methods
------------

instances([selector || element])  
instancesLatest()  
on, one, off, triggerHandler  
origins()  
setDefaults({})  

Events
------

after  
before  
close  
closing  
created  
destroy  
destroyed  
dismissable  
format  
geometry  
init  
state  
ready  
reposition  
repositioned  
scroll  
start  
startcancel  
startend  
updated

sideTip events
--------------

position  
positionTest  
positionTested