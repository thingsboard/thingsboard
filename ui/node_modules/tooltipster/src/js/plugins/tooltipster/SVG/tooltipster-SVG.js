var pluginName = 'tooltipster.SVG';

$.tooltipster._plugin({
	name: pluginName,
	core: {
		__init: function() {
			
			$.tooltipster._on('init', function(event) {
				
				var win = $.tooltipster._env.window;
				
				if (	win.SVGElement
					&&	event.origin instanceof win.SVGElement
				) {
					
					// auto-activation of the plugin on the instance
					event.instance._plug(pluginName);
				}
			});
		}
	},
	instance: {
		__init: function(instance) {
			
			var self = this;
			
			//list of instance variables
			self.__hadTitleTag = false;
			self.__instance = instance;
			
			// jQuery < v3.0's addClass and hasClass do not work on SVG elements.
			// However, $('.tooltipstered') does find elements having the class.
			if (!self.__instance._$origin.hasClass('tooltipstered')) {
				
				var c = self.__instance._$origin.attr('class') || '';
				
				if (c.indexOf('tooltipstered') == -1) {
					self.__instance._$origin.attr('class', c + ' tooltipstered');
				}
			}
			
			// if there is no content yet, let's look for a <title> child element
			if (self.__instance.content() === null) {
				
				// TODO: when there are several <title> tags (not supported in
				// today's browsers yet though, still an RFC draft), pick the right
				// one based on its "lang" attribute 
				var $title = self.__instance._$origin.find('>title');
				
				if ($title[0]) {
					
					var title = $title.text();
					
					self.__hadTitleTag = true;
					self.__instance._$origin.data('tooltipster-initialTitle', title);
					self.__instance.content(title);
					
					$title.remove();
				}
			}
			
			// rectify the geometry if SVG.js and its screenBBox plugin have been included
			self.__instance
				._on('geometry.'+ self.namespace, function(event) {
					
					var win = $.tooltipster._env.window;
					
					// SVG coordinates may need fixing but we need svg.screenbox.js
					// to provide it. SVGElement is IE8+
					if (win.SVG.svgjs) {
						
						if (!win.SVG.parser) {
							win.SVG.prepare();
						}
						
						var svgEl = win.SVG.adopt(event.origin);
						
						// not all figures need (and have) screenBBox
						if (svgEl && svgEl.screenBBox) {
							
							var bbox = svgEl.screenBBox();
							
							event.edit({
								height: bbox.height,
								left: bbox.x,
								top: bbox.y,
								width: bbox.width
							});
						}
					}
				})
				// if jQuery < v3.0, we have to remove the class ourselves
				._on('destroy.'+ self.namespace, function() {
					self.__destroy();
				});
		},
		
		__destroy: function() {
			
			var self = this;
			
			if (!self.__instance._$origin.hasClass('tooltipstered')) {
				var c = self.__instance._$origin.attr('class').replace('tooltipstered', '');
				self.__instance._$origin.attr('class', c);
			}
			
			self.__instance._off('.'+ self.namespace);
			
			// if the content was provided as a title tag, we may need to restore it
			if (self.__hadTitleTag) {
				
				// this must happen after Tooltipster restored (or not) the title attr
				self.__instance.one('destroyed', function() {
					
					// if a title attribute was restored, we just need to replace it with a tag
					var title = self.__instance._$origin.attr('title');
					
					if (title) {
						
						// must be namespaced to work
						$(document.createElementNS('http://www.w3.org/2000/svg', 'title'))
							.text(title)
							.appendTo(self.__instance._$origin);
						
						self.__instance._$origin.removeAttr('title');
					}
				});
			}
		}
	}
});

/* a build task will add "return $;" here */
