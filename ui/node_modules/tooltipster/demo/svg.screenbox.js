/*! svg.screenbbox.js - v0.1.1 - 2015-11-08
* Copyright (c) 2015 Ulrich-Matthias Schäfer; Licensed MIT */

;(function () {
	SVG.extend(SVG.Polyline, SVG.Polygon, SVG.Path, {
		
		screenBBox: function(){
			
			var i, transP
				, arr = this.array().valueOf().slice()   // path/point array
				, len = arr.length                       // length of array, we need it later
				, p   = this.doc().node.createSVGPoint() // instance of SVGPoint
				, m   = this.screenCTM().native()        // matrix for transfomation
				, pos = [,0,0]                           // default position (for V and H in Path)
				, transformPoints = function(arr){       // function to transform all points in arr
				
				// loop trough all coordinates and transform them by reference
				for(var i = 1, len = arr.length; i < len; i+=2){
					
					p.x = arr[i]
					p.y = arr[i+1]
					
					// transP is later used to get the last computed value
					transP = p.matrixTransform(m)
					
					arr[i]   = transP.x
					arr[i+1] = transP.y
				}
				
			}
			
			// kill all refereces to get a clean clone of the array
			for(i = 0; i < len; ++i){
				arr[i] = arr[i].slice()
			}
			
			// in case of polygon or polyline we need to normalize it to a path
			if(this instanceof SVG.Polygon || this instanceof SVG.Polyline){
				
				// just add the L
				while(i--){
					arr[i].unshift('L')
				}
				
				// first has to be M
				arr[0][0] = 'M'
				
			}
			
			// in case of polygon close the path accordingly
			if(this instanceof SVG.Polygon){
				arr.push(['z'])
			}
			
			// loop through path array and transform all points
			for(i = 0; i < len; ++i){
				
				// get path segment
				var s = arr[i][0]
				
				// coordinate list
				if (s == 'M' || s == 'L' || s == 'C' || s == 'S' || s == 'Q' || s == 'T'){
					pos = [,arr[i][1], arr[i][2]]
					transformPoints(arr[i])
					// We have to transform it to Line cause vertical and horiz0ntal lines could be rotated
				}else if (s == 'H' || s == 'V'){
					pos[s == 'H' ? 1 : 2] = arr[i][1]
					transformPoints(pos.slice())
					arr[i][0] = 'L'
					arr[i][1] = transP.x
					arr[i][2] = transP.y
					// position is the last one in arc
				}else if (s == 'A'){
					pos = [,arr[i][6], arr[i][7]]
					transformPoints(pos.slice())
					arr[i][6] = transP.x
					arr[i][7] = transP.y
				}
				
			}
			
			// return bbox of created path array
			return (new SVG.PathArray(arr)).bbox()
			
		}
		
	})
}).call(this);