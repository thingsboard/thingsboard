/* global jQuery */

// disable all events
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.trigger = function (options, parent) {
		this.init = function (el, options) {
			// do not forget parent
			parent.init.call(this, el, options);
			this._data.trigger.disabled = false;
		};
		this.trigger = function (ev, data) {
			if(!this._data.trigger.disabled) {
				parent.trigger.call(this, ev, data);
			}
		};
		this.disable_events = function () { this._data.trigger.disabled = true; };
		this.enable_events = function () { this._data.trigger.disabled = false; };
	};
})(jQuery);

// mapping
(function ($, undefined) {
	"use strict";
	// use this if you need any options
	$.jstree.defaults.mapper = {
		option_key : "option_value"
	};
	$.jstree.plugins.mapper = function () {
		this._parse_model_from_json = function (d, p, ps) {
			// d is the node from the server, it will be called recursively for children,
			// so you do not need to process at once
			/* // for example
			for(var i in d) {
				if(d.hasOwnProperty(i)) {
					d[i.toLowerCase()] = d[i];
				}
			}
			*/
			return parent._parse_model_from_json.call(this, d, p, ps);
		};
	};
})(jQuery);

// no hover
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.nohover = function () {
		this.hover_node = $.noop;
	};
})(jQuery);

// force multiple select
(function ($, undefined) {
	"use strict";
	$.jstree.defaults.multiselect = {};
	$.jstree.plugins.multiselect = function (options, parent) {
		this.activate_node = function (obj, e) {
			e.ctrlKey = true;
			parent.activate_node.call(this, obj, e);
		};
	};
})(jQuery);

// real checkboxes
(function ($, undefined) {
	"use strict";

	var inp = document.createElement("INPUT");
	inp.type = "checkbox";
	inp.className = "jstree-checkbox jstree-realcheckbox";

	$.jstree.defaults.realcheckboxes = {};

	$.jstree.plugins.realcheckboxes = function (options, parent) {
		this.bind = function () {
			parent.bind.call(this);
			this._data.realcheckboxes.uto = false;
			this.element
				.on('changed.jstree uncheck_node.jstree check_node.jstree uncheck_all.jstree check_all.jstree move_node.jstree copy_node.jstree redraw.jstree open_node.jstree ready.jstree loaded.jstree', $.proxy(function () {
						// only if undetermined is in setting
						if(this._data.realcheckboxes.uto) { clearTimeout(this._data.realcheckboxes.uto); }
						this._data.realcheckboxes.uto = setTimeout($.proxy(this._realcheckboxes, this), 50);
					}, this));
		};
		this.redraw_node = function(obj, deep, callback, force_draw) {
			obj = parent.redraw_node.call(this, obj, deep, callback, force_draw);
			if(obj) {
				var i, j, tmp = null, chk = inp.cloneNode(true);
				for(i = 0, j = obj.childNodes.length; i < j; i++) {
					if(obj.childNodes[i] && obj.childNodes[i].className && obj.childNodes[i].className.indexOf("jstree-anchor") !== -1) {
						tmp = obj.childNodes[i];
						break;
					}
				}
				if(tmp) {
					for(i = 0, j = tmp.childNodes.length; i < j; i++) {
						if(tmp.childNodes[i] && tmp.childNodes[i].className && tmp.childNodes[i].className.indexOf("jstree-checkbox") !== -1) {
							tmp = tmp.childNodes[i];
							break;
						}
					}
				}
				if(tmp && tmp.tagName === "I") {
					tmp.style.backgroundColor = "transparent";
					tmp.style.backgroundImage = "none";
					tmp.appendChild(chk);
				}
			}
			return obj;
		};
		this._realcheckboxes = function () {
			var ts = this.settings.checkbox.tie_selection;
			console.log(ts);
			$('.jstree-realcheckbox').each(function () {
				this.checked = (!ts && this.parentNode.parentNode.className.indexOf("jstree-checked") !== -1) || (ts && this.parentNode.parentNode.className.indexOf('jstree-clicked') !== -1);
				this.indeterminate = this.parentNode.className.indexOf("jstree-undetermined") !== -1;
				this.disabled = this.parentNode.parentNode.className.indexOf("disabled") !== -1;
			});
		};
	};
})(jQuery);

// no state
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.nostate = function () {
		this.set_state = function (state, callback) {
			if(callback) { callback.call(this); }
			this.trigger('set_state');
		};
	};
})(jQuery);

// no selected in state
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.noselectedstate = function (options, parent) {
		this.get_state = function () {
			var state = parent.get_state.call(this);
			delete state.core.selected;
			return state;
		};
	};
})(jQuery);

// additional icon on node (outside of anchor)
(function ($, undefined) {
	"use strict";
	var img = document.createElement('IMG');
	//img.src = "http://www.dpcd.vic.gov.au/__data/assets/image/0004/30667/help.gif";
	img.className = "jstree-questionmark";

	$.jstree.defaults.questionmark = $.noop;
	$.jstree.plugins.questionmark = function (options, parent) {
		this.bind = function () {
			parent.bind.call(this);
			this.element
				.on("click.jstree", ".jstree-questionmark", $.proxy(function (e) {
						e.stopImmediatePropagation();
						this.settings.questionmark.call(this, this.get_node(e.target));
					}, this));
		};
		this.teardown = function () {
			if(this.settings.questionmark) {
				this.element.find(".jstree-questionmark").remove();
			}
			parent.teardown.call(this);
		};
		this.redraw_node = function(obj, deep, callback, force_draw) {
			obj = parent.redraw_node.call(this, obj, deep, callback, force_draw);
			if(obj) {
				var tmp = img.cloneNode(true);
				obj.insertBefore(tmp, obj.childNodes[2]);
			}
			return obj;
		};
	};
})(jQuery);

// auto numbering
(function ($, undefined) {
	"use strict";
	var span = document.createElement('SPAN');
	span.className = "jstree-numbering";

	$.jstree.defaults.numbering = {};
	$.jstree.plugins.numbering = function (options, parent) {
		this.teardown = function () {
			if(this.settings.questionmark) {
				this.element.find(".jstree-numbering").remove();
			}
			parent.teardown.call(this);
		};
		this.get_number = function (obj) {
			obj = this.get_node(obj);
			var ind = $.inArray(obj.id, this.get_node(obj.parent).children) + 1;
			return obj.parent === '#' ? ind : this.get_number(obj.parent) + '.' + ind;
		};
		this.redraw_node = function(obj, deep, callback, force_draw) {
			var i, j, tmp = null, elm = null, org = this.get_number(obj);
			obj = parent.redraw_node.call(this, obj, deep, callback, force_draw);
			if(obj) {
				for(i = 0, j = obj.childNodes.length; i < j; i++) {
					if(obj.childNodes[i] && obj.childNodes[i].className && obj.childNodes[i].className.indexOf("jstree-anchor") !== -1) {
						tmp = obj.childNodes[i];
						break;
					}
				}
				if(tmp) {
					elm = span.cloneNode(true);
					elm.innerHTML = org + '. ';
					tmp.insertBefore(elm, tmp.childNodes[tmp.childNodes.length - 1]);
				}
			}
			return obj;
		};
	};
})(jQuery);

// additional icon on node (inside anchor)
(function ($, undefined) {
	"use strict";
	var _s = document.createElement('SPAN');
	_s.className = 'fa-stack jstree-stackedicon';
	var _i = document.createElement('I');
	_i.className = 'jstree-icon';
	_i.setAttribute('role', 'presentation');

	$.jstree.plugins.stackedicon = function (options, parent) {
		this.teardown = function () {
			this.element.find(".jstree-stackedicon").remove();
			parent.teardown.call(this);
		};
		this.redraw_node = function(obj, deep, is_callback, force_render) {
			obj = parent.redraw_node.apply(this, arguments);
			if(obj) {
				var i, j, tmp = null, icon = null, temp = null;
				for(i = 0, j = obj.childNodes.length; i < j; i++) {
					if(obj.childNodes[i] && obj.childNodes[i].className && obj.childNodes[i].className.indexOf("jstree-anchor") !== -1) {
						tmp = obj.childNodes[i];
						break;
					}
				}
				if(tmp) {
					if(this._model.data[obj.id].state.icons && this._model.data[obj.id].state.icons.length) {
						icon = _s.cloneNode(false);
						for(i = 0, j = this._model.data[obj.id].state.icons.length; i < j; i++) {
							temp = _i.cloneNode(false);
							temp.className += ' ' + this._model.data[obj.id].state.icons[i];
							icon.appendChild(temp);
						}
						tmp.insertBefore(icon, tmp.childNodes[0]);
					}
				}
			}
			return obj;
		};
	};
})(jQuery);

// selecting a node opens it
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.selectopens = function (options, parent) {
		this.bind = function () {
			parent.bind.call(this);
			this.element.on('select_node.jstree', function (e, data) { data.instance.open_node(data.node); });
		};
	};
})(jQuery);

// object as data
(function ($, undefined) {
	"use strict";
	$.jstree.defaults.datamodel = {};
	$.jstree.plugins.datamodel = function (options, parent) {
		this.init = function (el, options) {
			this._data.datamodel = {};
			parent.init.call(this, el, options);
		};
		this._datamodel = function (id, nodes, callback) {
			var i = 0, j = nodes.length, tmp = [], obj = null;
			for(; i < j; i++) {
				this._data.datamodel[nodes[i].getID()] = nodes[i];
				obj = {
					id : nodes[i].getID(),
					text : nodes[i].getText(),
					children : nodes[i].hasChildren()
				};
				if(nodes[i].getExtra) {
					obj = nodes[i].getExtra(obj); // icon, type
				}
				tmp.push(obj);
			}
			return this._append_json_data(id, tmp, $.proxy(function (status) {
				callback.call(this, status);
			}, this));
		};
		this._load_node = function (obj, callback) {
			var id = obj.id;
			var nd = obj.id === "#" ? this.settings.core.data : this._data.datamodel[obj.id].getChildren($.proxy(function (nodes) {
				this._datamodel(id, nodes, callback);
			}, this));
			if($.isArray(nd)) {
				this._datamodel(id, nd, callback);
			}
		};
	};
})(jQuery);
/*
	demo of the above
	function treeNode(val) {
		var id = ++treeNode.counter;
		this.getID = function () {
			return id;
		};
		this.getText = function () {
			return val.toString();
		};
		this.getExtra = function (obj) {
			obj.icon = false;
			return obj;
		};
		this.hasChildren = function () {
			return true;
		};
		this.getChildren = function () {
			return [
				new treeNode(Math.pow(val, 2)),
				new treeNode(Math.sqrt(val)),
			];
		};
	}
	treeNode.counter = 0;

	$('#jstree').jstree({
		'core': {
			'data': [
						new treeNode(2),
						new treeNode(3),
						new treeNode(4),
						new treeNode(5)
					]
		},
		plugins : ['datamodel']
	});
*/

// untested sample plugin to keep all nodes in the DOM
(function ($, undefined) {
	"use strict";
	$.jstree.plugins.dom = function (options, parent) {
		this.redraw_node = function (node, deep, is_callback, force_render) {
			return parent.redraw_node.call(this, node, deep, is_callback, true);
		};
		this.close_node = function (obj, animation) {
			var t1, t2, t, d;
			if($.isArray(obj)) {
				obj = obj.slice();
				for(t1 = 0, t2 = obj.length; t1 < t2; t1++) {
					this.close_node(obj[t1], animation);
				}
				return true;
			}
			obj = this.get_node(obj);
			if(!obj || obj.id === $.jstree.root) {
				return false;
			}
			if(this.is_closed(obj)) {
				return false;
			}
			animation = animation === undefined ? this.settings.core.animation : animation;
			t = this;
			d = this.get_node(obj, true);
			if(d.length) {
				if(!animation) {
					d[0].className = d[0].className.replace('jstree-open', 'jstree-closed');
					d.attr("aria-expanded", false);
				}
				else {
					d
						.children(".jstree-children").attr("style","display:block !important").end()
						.removeClass("jstree-open").addClass("jstree-closed").attr("aria-expanded", false)
						.children(".jstree-children").stop(true, true).slideUp(animation, function () {
							this.style.display = "";
							t.trigger("after_close", { "node" : obj });
						});
				}
			}
			obj.state.opened = false;
			this.trigger('close_node',{ "node" : obj });
			if(!animation || !d.length) {
				this.trigger("after_close", { "node" : obj });
			}
		};
	};
})(jQuery);

// customize plugin by @Lusito
// https://github.com/Lusito/jstree/blob/node-customize/src/jstree-node-customize.js
/**
 * ### Node Customize plugin
 *
 * Allows to customize nodes when they are drawn.
 */
(function (factory) {
	"use strict";
	if (typeof define === 'function' && define.amd) {
		define('jstree.node_customize', ['jquery','jstree'], factory);
	}
	else if(typeof exports === 'object') {
		factory(require('jquery'), require('jstree'));
	}
	else {
		factory(jQuery, jQuery.jstree);
	}
}(function ($, jstree, undefined) {
	"use strict";

	if($.jstree.plugins.node_customize) { return; }

	/**
	 * the settings object.
	 * key is the attribute name to select the customizer function from switch.
	 * switch is a key => function(el, node) map.
	 * default: function(el, node) will be called if the type could not be mapped
	 * @name $.jstree.defaults.node_customize
	 * @plugin node_customize
	 */
	$.jstree.defaults.node_customize = {
		"key": "type",
		"switch": {},
		"default": null
	};

	$.jstree.plugins.node_customize = function (options, parent) {
		this.redraw_node = function (obj, deep, callback, force_draw) {
			var el = parent.redraw_node.apply(this, arguments);
			if (el) {
				var node = this.get_node(obj);
				var cfg = this.settings.node_customize;
				var key = cfg.key;
				var type =  (node && node.original && node.original[key]);
				var customizer = (type && cfg.switch[type]) || cfg.default;
				if(customizer)
					customizer(el, node);
			}
			return el;
		};
	}
}));


// parentsload plugin by @ashl1
/**
 * ### Parentsload plugin
 *
 * Change load_node() functionality in jsTree, to possible load not yes downloaded node with all it parent in a single request (only useful with lazy loading).
 *
 * version 1.0.0 (Alexey Shildyakov - ashl1future@gmail.com)
 * 2015: Compatible with jsTree-3.2.1
 */
/*globals jQuery, define, exports, require, document */
(function (factory) {
        "use strict";
        if (typeof define === 'function' && define.amd) {
                define('jstree.parentsload', ['jquery','jstree'], factory);
        }
        else if(typeof exports === 'object') {
                factory(require('jquery'), require('jstree'));
        }
        else {
                factory(jQuery, jQuery.jstree);
        }
}(function ($, jstree, undefined) {
        "use strict";

        if($.jstree.plugins.parentsload) { return; }

        /**
         * parentsload configuration
         *
         * The configuration syntax is almost the same as for core.data option. You must set parenstload.data the following:
         *
         * parentsload: {
         *      data: function(){} // this function overwrites core data.data options
         * }
         *
         * OR
         *
         * parentsload: {
         *      data: {
         *              url: function(node){} OR string,
         *              data: function(node){} OR associative array as json{data} jQuery parameter
         *      }
         * }
         *
         * In last case at least on of 'url' or 'data' must be presented.
         *
         * At first, the plugin load_node() detects if the node already downloaded. If is - uses the core.data settings, if not - uses parentsload.data settings
         * to fetch in one query the specified node and all its parent. The data must be in the first mentioned JSON format with set nested children[].
         * Each node level should consist of all nodes on the level to properly work with the tree in the future. Otherwise, you must manually call load_node
         * on every parent node to fetch all children nodes on that level.
         *
         * @name $.jstree.defaults.parentsload
         * @plugin parentsload
         */
        $.jstree.defaults.parentsload = null;
        $.jstree.plugins.parentsload = function (options, parent) {
                this.init = function (el, options) {
                        parent.init.call(this, el, options);
                        this.patch_data()
                };
                this.patch_data = function(){
                        var parentsloadSettings = this.settings.parentsload;
                        var jsTreeDataSettings = this.settings.core.data;
                        var self = this;

                        var callError = function(number, message) {
                                self._data.core.last_error = { 'error' : 'configuration', 'plugin' : 'parentsload', 'id' : 'parentsload_' + number, 'reason' : message, 'data' : JSON.stringify({config: parentsloadSettings}) };
                                self.settings.core.error.call(self, self._data.core.last_error);
                        }

                        if(!parentsloadSettings) {
                                callError('01', 'The configuration must be presented')
                                return
                        }
                        parentsloadSettings = parentsloadSettings.data;

                        var patchSettingsProperty = function (propertyName) {
                                var property = parentsloadSettings[propertyName],
                                    coreProperty = jsTreeDataSettings[propertyName];
                                if (property) {
                                        jsTreeDataSettings[propertyName] = function(node) {
                                                if (this.get_node(node).parentsload_required) {
                                                        if ($.isFunction(property)) {
                                                                return property.call(this, node)
                                                        } else {// (typeof property === 'string')
                                                                return property
                                                        }
                                                } else {
                                                        if ($.isFunction(coreProperty)) {
                                                                return coreProperty.call(this, node)
                                                        } else { // (typeof coreProperty === 'string')
                                                                return coreProperty
                                                        }
                                                }
                                        }
                                } /* else {
                                        use jstree the same data[propertyName] settings
                                }*/
                        }

                        if($.isFunction(parentsloadSettings)) {
                                this.settings.data = parentsloadSettings
                        } else if (typeof parentsloadSettings === 'object') {
                                if (! (parentsloadSettings.url || parentsloadSettings.data)) {
                                        callError('02', 'The "data.url" or "data.data" must be presented in configuration')
                                        return
                                }
                                patchSettingsProperty('url')
                                patchSettingsProperty('data')

                        } else {
                                callError('03', 'The appropriate "data.url" or "data.data" must be presented in configuration')
                        }
                }

                this.load_node = function (obj, callback) {
                        if($.isArray(obj)) {
                                // FIXME: _load_nodes will not load nodes not presented in the tree
                                this._load_nodes(obj.slice(), callback);
                                return true;
                        }
                        var foundObj = this.get_node(obj);
                        if (foundObj) {
                                return parent.load_node.apply(this, arguments)
                        } else {
                                // node hasn't been loaded
                                var id = obj.id? obj.id: obj;
                                this._model.data[id] = {
                                        id : id,
                                        parent : '#',
                                        parents : [],
                                        children : [],
                                        children_d : [],
                                        state : { loaded : false },
                                        li_attr : {},
                                        a_attr : {},
                                        parentsload_required : true,
                                };
                                return parent.load_node.call(this, obj, function(obj, status){
                                        obj.parentsload_required = !status
                                        callback.call(this, obj, status)
                                })
                        }
                }
        };
}));

// conditional deselect
(function (factory) {
	"use strict";
	if (typeof define === 'function' && define.amd) {
		define('jstree.conditionaldeselect', ['jquery','jstree'], factory);
	}
	else if(typeof exports === 'object') {
		factory(require('jquery'), require('jstree'));
	}
	else {
		factory(jQuery, jQuery.jstree);
	}
}(function ($, jstree, undefined) {
	"use strict";

	if($.jstree.plugins.conditionaldeselect) { return; }
	$.jstree.defaults.conditionaldeselect = function () { return true; };
	$.jstree.plugins.conditionaldeselect = function (options, parent) {
		// own function
		this.deselect_node = function (obj, supress_event, e) {
			if(this.settings.conditionaldeselect.call(this, this.get_node(obj), e)) {
				return parent.deselect_node.call(this, obj, supress_event, e);
			}
		};
	};

}));

// conditional close
(function (factory) {
	"use strict";
	if (typeof define === 'function' && define.amd) {
		define('jstree.conditionalclose', ['jquery','jstree'], factory);
	}
	else if(typeof exports === 'object') {
		factory(require('jquery'), require('jstree'));
	}
	else {
		factory(jQuery, jQuery.jstree);
	}
}(function ($, jstree, undefined) {
	"use strict";

	if($.jstree.plugins.conditionalclose) { return; }
	$.jstree.defaults.conditionalclose = function () { return true; };
	$.jstree.plugins.conditionalclose = function (options, parent) {
		// own function
		this.close_node = function (obj, animation) {
			if(this.settings.conditionalclose.close.call(this, this.get_node(obj), e)) {
				return parent.deselect_node.call(this, obj, animation);
			}
		};
	};

}));

// separate items and badges plugin by vdkkia (vahidkiani88@gmail.com)
// https://github.com/vdkkia/jstree
//
//CSS:
//.separator{border-bottom:1px solid;border-image-source:linear-gradient(45deg,rgba(0,0,0,0),rgba(0,0,0,.1),rgba(0,0,0,0));border-image-slice:1;width:100%;left:0;color:#aaa;font-size:10px;font-weight:400;float:right;text-align:right;padding-right:20px;position:absolute;z-index:-1}.treeaction{color:#555;margin-left:3px;padding:2px;font-weight:700;font-size:10px;border:none;background-color:#fff;transition:all .2s ease-in-out;text-decoration:none;float:right;margin-right:2px;top:4px}.treeaction:hover{color:green;text-decoration:none;transform:scale(1.5)}
(function (factory) {
}(function ($, jstree, undefined) {
	"use strict";
	$.jstree.plugins.node_customize = function (options, parent) {
		this.redraw_node = function (obj, deep, callback, force_draw) {
			var el = parent.redraw_node.apply(this, arguments);
			if (el) {
				var node = this.get_node(obj);
				var cfg = this.settings.node_customize;
				var key = cfg.key;
				var type =  (node && node.original && node.original[key]);
				var customizer = (type && cfg.switch[type]) || cfg.default;
				if(customizer)
					customizer(el, node);
			}
			return el;
		};
	}
}));
