$(function() {
	
	// menu
	
	$('header select').change(function() {
		var goTo = $(this).val();
		var section = $('#'+goTo);
		var offset = section.offset().top;
		$('html, body').scrollTop(offset);
	});
	
	// usual tooltips
	
	$('.tooltip').not('#welcome .tooltip').tooltipster();
	
	$('#welcome .tooltip').tooltipster({
		theme: 'tooltipster-light'
	});
	
	// demos
	
	$('#demo-default').tooltipster({});
	
	$('#demo-smart').draggable({
		grid: [30, 30],
		helper: 'clone',
		revert: true,
		scroll: false,
		start: function(event, ui) {
			ui.helper
				.tooltipster({
					content: 'Move my origin next to the edges of the screen and see how I adapt.<br />Besides, there are several options to tune my behavior.',
					contentAsHTML: true,
					trackerInterval: 10,
					trackOrigin: true,
					trigger: 'custom'
				})
				.tooltipster('open');
		},
		stop: function(event, ui) {
			ui.helper.tooltipster('destroy');
		}
	});
	
	$('#demo-html').tooltipster({
		// setting a same value to minWidth and maxWidth will result in a fixed width
		maxWidth: 400,
		side: 'right'
	});
	
	$('#demo-theme').tooltipster({
		animation: 'grow',
		theme: 'tooltipster-pink'
	});
	
	$('#demo-callback').tooltipster({
		content: 'Loading...',
		updateAnimation: false,
		functionBefore: function(instance, helper) {
			
			var $origin = $(helper.origin);
			
			if ($origin.data('ajax') !== 'cached') {
				
				$.jGFeed(
					'http://ws.audioscrobbler.com/2.0/user/ce3ge/recenttracks.rss?',
					function(feeds){
						
						if(!feeds){
							instance.content('Woops - there was an error retrieving my last.fm RSS feed');
						}
						else {
							
							instance.content($('<span>I last listened to: <strong>' + feeds.entries[0].title + '</strong></span>'));
							
							$origin.data('ajax', 'cached');
						}
					},
					10
				);
				
				$origin.data('ajax', 'cached');
			}
		},
		functionAfter: function(instance) {
			alert('The tooltip has closed!');
		}
	});
	
	$('#demo-events').tooltipster({
		trigger: 'click'
	});
	
	/*
	// for testing purposes
	var instance = $('#demo-events').tooltipster('instance');
	instance.on('reposition', function(){
		alert('hey');
	});
	*/
	
	$(window).keypress(function() {
		$('#demo-events').tooltipster('hide');
	});
	
	$('#demo-interact').tooltipster({
		contentAsHTML: true,
		interactive: true
	});
	
	$('#demo-touch').tooltipster({
		trigger: 'click',
		functionBefore: function(instance, helper){
			if (helper.event.type == 'click') {
				instance.content('You opened me with a regular mouse click :)');
			}
			else {
				instance.content('You opened me by a tap on the screen :)');
			}
		}
	});
	$('#demo-imagemaparea').tooltipster();
	
	$('#demo-multiple').tooltipster({
		animation: 'swing',
		content: 'North',
		side: 'top',
		theme: 'tooltipster-borderless'
	});
	$('#demo-multiple').tooltipster({
		content: 'East',
		multiple: true,
		side: 'right',
		theme: 'tooltipster-punk'
	});	
	$('#demo-multiple').tooltipster({
		animation: 'grow',
		content: 'South',
		multiple: true,
		side: 'bottom',
		theme: 'tooltipster-light'
	});	
	$('#demo-multiple').tooltipster({
		animation: 'fall',
		content: 'West',
		multiple: true,
		side: 'left',
		theme: 'tooltipster-shadow'
	});
	
	var complexInterval;
	
	$('#demo-complex')
		.tooltipster({
			trackerInterval: 15,
			trackOrigin: true,
			trigger: 'custom'
		})
		.click(function(){
			
			var $this = $(this);
			
			if($this.hasClass('complex')){
				
				$this
					.removeClass('complex')
					.tooltipster('hide')
					.css({
						left: '',
						top: ''
					});
				
				clearInterval(complexInterval);
			}
			else {
				
				var bcr = this.getBoundingClientRect(),
					odd = true;
				
				$this
					.addClass('complex')
					.css({
						left: bcr.left + 'px',
						top: bcr.top + 'px'
					})
					.tooltipster('show');
				
				complexInterval = setInterval(function(){
					
					var offset = odd ? 200 : 0;
					
					$this.css({
						left: bcr.left + offset
					});
					
					odd = !odd;
				}, 2000);
			}
		});
	
	$('#demo-position').tooltipster({
		// 8 extra pixels for the arrow to overflow the grid
		functionPosition: function(instance, helper, data){
			
			// this function is pretty dumb and does not check if there is actually
			// enough space available around the tooltip to move it, it just makes it
			// snap to the grid. You might want to do something smarter in your app!
			
			var gridBcr = $('#demo-position-grid')[0].getBoundingClientRect(),
				arrowSize = parseInt($(helper.tooltipClone).find('.tooltipster-box').css('margin-left'));
			
			// override these
			data.coord = {
				// move the tooltip so the arrow overflows the grid
				left: gridBcr.left - arrowSize,
				top: gridBcr.top
			};
			
			return data;
		},
		maxWidth: 228,
		side: ['right']
	});
	
	$('#demo-plugin').tooltipster({
		plugins: ['follower']
	});
	
	// nested demo
	$('#nesting').tooltipster({
		content: $('<span>Hover me too!</span>'),
		functionReady: function(instance){
			
			// the nested tooltip must be initialized once the first
			// tooltip is open, that's why we do this inside
			// functionReady()
			instance.content().tooltipster({
				content: 'I am a nested tooltip!',
				distance: 0
			});
		},
		interactive: true
	});
	
	// grouped demo
	$('.tooltip_slow').tooltipster({
		animationDuration: 1000,
		delay: 1000
	});
	
	$.tooltipster.group('tooltip_group');
	
	// themes
	
	$('.tooltipster-light-preview').tooltipster({
		theme: 'tooltipster-light'
	});
	$('.tooltipster-borderless-preview').tooltipster({
		theme: 'tooltipster-borderless'
	});
	$('.tooltipster-punk-preview').tooltipster({
		theme: 'tooltipster-punk'
	});
	$('.tooltipster-noir-preview').tooltipster({
		theme: 'tooltipster-noir'
	});
	$('.tooltipster-shadow-preview').tooltipster({
		theme: 'tooltipster-shadow'
	});
	
	prettyPrint();
});