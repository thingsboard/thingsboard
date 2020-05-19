# Tooltipster plugin creation guide

[TL;DR](#tldr)  
1. [Find a name for your plugin](#naming)  
2. [Determine if you'll work at core or instance level, or both](#level)  
3. [The `__init` and `__destroy` methods](#magic)  
4. [Create your public, protected and private methods](#methods)  
5. [Use Tooltipster's protected methods](#tooltipster)  
6. [Use Tooltipster's events](#events)  
7. [Create new options](#options)  
8. [If your plugin includes CSS ](#css)  
9. [Give user instructions](#installation)  
10. [Conventions and good practices](#goodpractices)  
11. [The full, typical template for plugins](#template)  
12. [Examples](#examples)  
   1. [Basic => 1 core method, 1 instance method, no options](#examples.basic)  
   2. [Auto-enable your plugin on tooltips](#examples.plug)

## <a name="tldr"></a>TL;DR

Your plugin might look like this:

```javascript
    $.tooltipster._plugin({
        name: 'namespace.pluginName',
        core: {
            __init: function(core) { ... },
            myNewCoreMethod: function() { ... },
            // double underscore please
            __somePrivateMethod: function() { ... }
        },
        instance: {
            __init: function(instance) { ... },
            __destroy: function() { ... },
            myNewInstanceMethod: function() { ... },
            // double underscore please
            __somePrivateMethod: function() { ... }
        }
    });
```

Now let's start over with explanations.

## <a name="naming"></a>1. Find a name for your plugin

The plugin name must be namespaced in order to resolve conflicts in case somebody writes another plugin of the same name. Use your initials or something random. Preventing conflicts is explained in the [Plugins](http://iamceege.github.io/tooltipster/#plugins) section of the documentation.

## <a name="level"></a>2. Determine if you'll work at core or instance level, or both

In Tooltipster, there is a core and there are instances. Each tooltip is associated to an instance, while the core is a single object registered as `$.tooltipster`. In your plugin, you can add methods to the core and/or instances.

As a matter of principle, a tooltip should only care about itself and should never interact with other tooltips. Anything that concerns several tooltips at once should be handled at core level. That's why methods like `setDefaults` or `instances` are implemented at core level. If tooltips need to be created at some point, it should also be done from the core.

The [discovery](https://github.com/louisameline/tooltipster-discovery/blob/master/tooltipster-discovery.js) plugin is a real life example of plugin that works at core level to create "synchronized" tooltips.  
The [scrollableTip](https://github.com/louisameline/tooltipster-scrollableTip/blob/master/tooltipster-scrollableTip.js) plugin is an example of plugin that works at instance level to keep the tooltip inside the viewport.

## <a name="magic"></a>3. The `__init` and `__destroy` methods

The special private `__init` method of your plugin, if it exists, will be called automatically:

* at core level: when you register your plugin  
* at instance level: when a tooltip is initialized (if your plugin is enabled for that tooltip), or when you plug your plugin manually on an existing instance.

The `__init` methods get the object for which they are instantiated as parameter, either the core or an instance. Your methods will be called in the context of your plugin, not the context of the object, so you will probably want to store this reference (check the examples at the end).

The special private `__destroy` method of your plugin, if it exists, will be called automatically:

* At instance level only, upon destruction of the tooltip OR when you unplug your plugin from the instance manually. If you have unbindings to do, don't forget them to prevent memory leaks

## <a name="methods"></a>4. Create your public, protected and private methods

* Methods that do not start with an underscore are public, which means that the user will be able to call them, just like any of the native methods described in the [Methods](http://iamceege.github.io/tooltipster/#methods) section of the general documentation  
* Methods that start with a single underscore are protected, which means that the user *should not* call them, but that other plugins may. Unless you plan on creating a plugin that can interact with others, you should not use them
* Methods that start with a double underscore are private. The user won't be able to call them, and other plugins shouldn't try to

If two plugins add public/protected methods of the same name at the same level (core or instance), there will be a conflict. The way to resolve conflicts is described in the [plugins](http://iamceege.github.io/tooltipster/#plugins) section general documentation. There can be no conflicts between private methods.

## <a name="tooltipster"></a>5. Use Tooltipster's protected methods

Aside from its documented public methods, Tooltipster also has protected and private ones. Don't use the private ones as they are considered internal and may change without notice. The protected ones on the other hand are here for plugin makers to use:

### At core level (`$.tooltipster.methodName`):

`_plugin`,  
`_getRuler`,  
`_on`, `_one`, `_off`, `_trigger`

* `_plugin` is used to register your plugin
* A call to `_getRuler` returns an object that measures an element and tells you if its content will overflow if you resize it. It's useful when you deal with positioning. Read the source for more info
* The other methods are similar to their public equivalent, except that your listeners will be called before the user's, and will be protected from accidental unbinding. You should always use them instead of the public ones

There is also one core protected property that you can use: `$.tooltipster._env`. It's an object of this form:

```javascript
{
	hasTouchCapability: boolean,
	// CSS transition support
	hasTransitions: boolean,
	// IE version
	IE: false || int,
	// Tooltipster's version
	semVer: 'x.x.x',
	// a reference to the (supposedly) global window object, if like me you don't like to
	// work with a global inside a UMD module. Might be useful for testing purposes too.
	window: object
}
```

### At instance level (`instance.methodName`):

`_close`, `_open` `_openShortly`,  
`_on`, `_one`, `_off`, `_trigger`,  
`_optionsExtract`,  
`_plug`, `_unplug`,  
`_touchIsEmulatedEvent`, `_touchIsMeaningfulEvent`, `_touchIsTouchEvent`, `_touchRecordEvent`, `_touchSwiped`

* `_close` and `_open` are similar to their public equivalent, except that it lets you pass an event as first parameter in case you want to add new triggers
* `_openShortly` can be used to have a delayed opening, like the `hover` trigger
* The event methods: same as at core level
* `_optionsExtract` must be used if you offer new options. See the [Create new options](#options) section below
* `_plug` and `_unplug` methods can be used to enable/disable a plugin manually on a given instance. See the [example](#examples.plug) below.
* The `touch*` methods are used to handle touch devices, for example when you want to differentiate a genuine click event from a click event emulated after a tap

There are also two instance protected properties that you can use: `instance._$tooltip` and `instance._$origin`. They are the jQuery-wrapped tooltip and origin root HTML elements.

## <a name="events"></a>6. Use Tooltipster's events

When something happens in Tooltipster, events get fired on the instance and/or core emitters. Most of the time, that's how you will add features: listening for a type of event and reacting to it. All events are listed in the [Events](http://iamceege.github.io/tooltipster/#events) section of the general documentation. And don't forget to use the protected event methods listed above.

For example, when a tooltip must be opened, Tooltipster's main script does nothing but send a `reposition` event. Then it's `sideTip` who listens to this event and positions the tooltip on a side of the origin, and sends a `repositioned` event when it's done. When `follower` is used instead of `sideTip`, it does more or less the same thing.

## <a name="options"></a>7. Create new options

Your plugin might offer new options to the user. When he initializes a tooltip, he has two options:

* Simply use them like the standard options:
```javascript
$el.tooltipster({
    side: 'top',
    myNewOption: 'value'
})
```
* Or namespace them to prevent conflicts with other plugins:  
```javascript
$el.tooltipster({
    side: 'top',
    'myNamespace.myPlugin': {
        myNewOption: 'value'
    }
})
```

Note: there is no built-in options system at core level, only at instance level.

In your plugin, you have to call `instance.option('optionName')` to know the value of a standard option. But since you don't know how your own options will be declared, you have to use Tooltipster's `_optionsExtract` protected method to get them easily. `_optionsExtract` takes the full name of your plugin as first parameter, and the default values of your options as second parameter.

```javascript

var pluginName = 'namespace.myPlugin';

$.tooltipster._plugin({
    name: pluginName,
    instance: {
        __init: function(instance) {
            
            var defaultOptions = {
                    myNewOption: 'value',
                    myNewOption2: 'value'
                },
                myOwnOptions = instance._optionsExtract(pluginName, defaultOptions);
        }
    }
```

That works well, but the user might change the value of one of your options after initialization with an `instance.option` method call. In this case, set a listener for option changes to reload your options every time:

```javascript

var pluginName = 'namespace.myPlugin';

$.tooltipster._plugin({
    name: pluginName,
    instance: {
        __init: function(instance) {
            
            var self = this;
            
            self.__instance = instance;
            self.__myOwnOptions;
            // let's namespace our listeners for specific unbinding later
            self.__namespace = pluginName+ '-' +Math.round(Math.random()*1000000);
            
            // initial options loading
            self.__reloadOptions();
            
            // reload at every future options changes
            self.__instance._on('options.'+ self.__namespace, function() {
                self.__reloadOptions();
            });
        },
        __destroy: function() {
            // unbind our listeners
            this.__instance._off('.'+ self.__namespace);
        },
        __reloadOptions: function() {
            
            var defaultOptions = {
                myNewOption: 'value',
                myNewOption2: 'value'
            };
            
            this.__myOwnOptions = this.__instance._optionsExtract(pluginName, defaultOptions);
        }
    }
});
```

## <a name="css"></a>8. If your plugin includes CSS 

If you write CSS for the tooltips that will use your plugin, you must "namespace" all your properties.

Why? Imagine that you plugin makes the tooltip contents pink with `.tooltipster-content { color: pink }`. When the CSS file is loaded, that rule will apply to all tooltips in the page, not just the tooltips that have your plugin enabled.

The solution is that you add a `.tooltipster-myPlugin` class to the root HTML element of the tooltip, typically like this:
```javascript
    __init: function(instance) {
        instance._$tooltip.addClass('tooltipster-myPlugin');
    }
```
 
 and then write in your CSS: `.tooltipster-myPlugin .tooltipster-content { color: pink }`.  
 That's how it's done in [follower](https://github.com/louisameline/tooltipster-follower/blob/master/src/css/tooltipster-follower.css) for example.

## <a name="installation"></a>9. Give user instructions

Tell your users to include your plugin file in their page after the main Tooltipster script.

Remind them that, in order to use your new instance methods (if you offer any), they have to declare your plugin in the options of their tooltips, for example like this:

```javascript
$('.tooltip').tooltipster({
    // don't let them forget that a display plugin like the default sideTip is required too
    plugin: ['sideTip', 'yourPlugin']
});
```

Keep things simple and don't tell them to declare it as `'yourNamespace.yourPlugin'`, even if it would work. If your users run into a conflict with another plugin, tell them to read the [Plugins](http://iamceege.github.io/tooltipster/#plugins) section of the documentation.

## <a name="goodpractices"></a>10. Conventions and good practices

* If your plugin is called `myNamespace.myPluginName`, the name of its file should be `tooltipster-myPluginName.js`. When publishing to GitHub, Npm or somewhere else, also name your project `tooltipster-myPluginName`.
* Have your public methods return the object for which they are instantiated (either the core or an instance) to make calls chainable, unless of course they're supposed to return something else
* Namespace your listeners (if you have any) to prevent accidentally unbinding listeners that belong to the user or to another plugin. Also, unbind your listeners in the `__destroy` method.
* Make your plugin UMD compliant. It means that your plugin should be wrapped like this:

```javascript
(function(root, factory) {
	if (typeof define === 'function' && define.amd) {
		define(['tooltipster'], function($) {
			return (factory($));
		});
	}
	else if (typeof exports === 'object') {
		module.exports = factory(require('tooltipster'));
	}
	else {
		factory(jQuery);
	}
}(this, function($) {

	// your $.tooltipster._plugin() code here
}
```

## <a name="template"></a>11. The full, typical template for plugins

Summing up what we saw previously, a plugin which offers new methods at both core and instance levels, plus new options, would typically look like the following. Look for the uppercase stuff to edit:

```javascript
(function(root, factory) {
    if (typeof define === 'function' && define.amd) {
        define(['tooltipster'], function($) {
            return (factory($));
        });
    }
    else if (typeof exports === 'object') {
        module.exports = factory(require('tooltipster'));
    }
    else {
        factory(jQuery);
    }
}(this, function($) {

    var pluginName = 'NAMESPACE.PLUGINNAME';
    
    $.tooltipster._plugin({
        name: pluginName,
        core: {
            __init: function(core) {
                
                this.__core = core;
                
                /* YOUR CODE HERE */
            },
            MYCOREPUBLICMETHOD: function() {
                
                /* YOUR CODE HERE */
                
                return this.__core;
            }
        },
        instance: {
            __defaults: function() {
                
                return {
                     /* YOUR DEFAULT OPTIONS HERE */
                };
            },
            __init: function(instance) {
                
                var self = this;
                
                self.__instance = instance;
                // let's namespace our listeners for specific unbinding later
                self.__namespace = pluginName+ '-' +Math.round(Math.random()*1000000);
                self.__options;
                
                // initial options loading
                self.__reloadOptions();
                
                // reload at every future options changes
                self.__instance._on('options.'+ self.__namespace, function() {
                    self.__reloadOptions();
                });
                
                /* YOUR CODE HERE */
            },
            __destroy: function() {
                
                // unbind our listeners
                this.__instance._off('.'+ self.__namespace);
                
                /* YOUR CODE HERE */
            },
            __reloadOptions: function() {
                this.__options = this.__instance._optionsExtract(pluginName, this.__defaults());
            },
            MYPUBLICINSTANCEMETHOD: function(){
                
                /* YOUR CODE HERE */
                
                return this.__instance;
            }
        }
    });
}
```


## <a name="examples"></a>12. Examples


### <a name="examples.basic"></a>12.1. Basic => 1 core method, 1 instance method, no options

Let's create a plugin that allows to close all tooltips at once, and that also lets you open a tooltip without animation.

First, declare your plugin in a new file:

```javascript
// for clarity, I won't include the UMD wrapper here, but you should
$.tooltipster._plugin({
    name: 'namespace.myPlugin',
    core: {
        __init: function(core) {
            // this reference is the same as $.tooltipster, so it's not actually very useful
            this.__core = core;
        },
        closeAll: function() {
            
            var instances = this.__core.instances();
            
            $.each(instances, function(i, instance) {
                instance.close();
            });
            
            this.__log();
            
            return this.__core;
        },
        __log: function() {
            console.log('Closed all tooltips in the page');
        }
    },
    instance: {
        __init: function(instance) {
            this.__instance = instance;
        },
        openWithoutAnimation: function() {
        
            var animationDuration = this.__instance.option('animationDuration');
        
            this.__instance
                // disable animation
                .option('animationDuration', 0)
                .open()
                // restore previous animationDuration for future openings
                .option('animationDuration', animationDuration);
            
            return this.__instance;
        }
    };
});
```

Then include your plugin file in the HTML page (after the main Tooltipster script) and start using it:

```javascript
$('#tooltip')
    .tooltipster({
        // enable your plugin on this tooltip
        plugin: ['sideTip', 'myPlugin']
    })
    .tooltipster('openWithoutAnimation');

// closes and logs 'Closed all tooltips in the page'
$.tooltipster.closeAll();
```

### <a name="examples.plug"></a>12.2. Auto-enable your plugin on tooltips

If your plugin has instance methods, it will automatically be plugged upon initialization in instances which have it listed in their `plugins` option.

Sometimes it's fine because you want your plugin enabled only if the user explicitly asks for it. For example the `follower` plugin should not be enabled on all tooltips, in case the user wants to use `sideTip` on some of them.

But sometimes there is no harm in enabling a plugin on all tooltips and save the user the trouble of having to list it in the `plugins` option. That's the case for the [`SVG`](https://github.com/iamceege/tooltipster/blob/master/src/js/plugins/tooltipster/SVG/tooltipster-SVG.js) plugin which improves Tooltipster in case the origin is an SVG element, and does nothing if it's not. To achieve it, we just listen to the core for newly created instances and manually plug ourselves on these instances.

```javascript

var pluginName = 'namespace.myPlugin'

$.tooltipster._plugin({
    name: pluginName,
    core: {
        __init: function(core) {
        
            core._on('init', function(event) {
                event.instance._plug(pluginName);
            });
        }
    }
    instance: {
        ...
    }
});
```