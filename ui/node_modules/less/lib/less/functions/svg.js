import Dimension from '../tree/dimension';
import Color from '../tree/color';
import Expression from '../tree/expression';
import Quoted from '../tree/quoted';
import URL from '../tree/url';

export default environment => {
    return { 'svg-gradient': function(direction) {
        let stops;
        let gradientDirectionSvg;
        let gradientType = 'linear';
        let rectangleDimension = 'x="0" y="0" width="1" height="1"';
        const renderEnv = {compress: false};
        let returner;
        const directionValue = direction.toCSS(renderEnv);
        let i;
        let color;
        let position;
        let positionValue;
        let alpha;

        function throwArgumentDescriptor() {
            throw { type: 'Argument',
                message: 'svg-gradient expects direction, start_color [start_position], [color position,]...,' +
                            ' end_color [end_position] or direction, color list' };
        }

        if (arguments.length == 2) {
            if (arguments[1].value.length < 2) {
                throwArgumentDescriptor();
            }
            stops = arguments[1].value;
        } else if (arguments.length < 3) {
            throwArgumentDescriptor();
        } else {
            stops = Array.prototype.slice.call(arguments, 1);
        }

        switch (directionValue) {
            case 'to bottom':
                gradientDirectionSvg = 'x1="0%" y1="0%" x2="0%" y2="100%"';
                break;
            case 'to right':
                gradientDirectionSvg = 'x1="0%" y1="0%" x2="100%" y2="0%"';
                break;
            case 'to bottom right':
                gradientDirectionSvg = 'x1="0%" y1="0%" x2="100%" y2="100%"';
                break;
            case 'to top right':
                gradientDirectionSvg = 'x1="0%" y1="100%" x2="100%" y2="0%"';
                break;
            case 'ellipse':
            case 'ellipse at center':
                gradientType = 'radial';
                gradientDirectionSvg = 'cx="50%" cy="50%" r="75%"';
                rectangleDimension = 'x="-50" y="-50" width="101" height="101"';
                break;
            default:
                throw { type: 'Argument', message: 'svg-gradient direction must be \'to bottom\', \'to right\',' +
                    ' \'to bottom right\', \'to top right\' or \'ellipse at center\'' };
        }
        returner = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"><${gradientType}Gradient id="g" ${gradientDirectionSvg}>`;

        for (i = 0; i < stops.length; i += 1) {
            if (stops[i] instanceof Expression) {
                color = stops[i].value[0];
                position = stops[i].value[1];
            } else {
                color = stops[i];
                position = undefined;
            }

            if (!(color instanceof Color) || (!((i === 0 || i + 1 === stops.length) && position === undefined) && !(position instanceof Dimension))) {
                throwArgumentDescriptor();
            }
            positionValue = position ? position.toCSS(renderEnv) : i === 0 ? '0%' : '100%';
            alpha = color.alpha;
            returner += `<stop offset="${positionValue}" stop-color="${color.toRGB()}"${alpha < 1 ? ` stop-opacity="${alpha}"` : ''}/>`;
        }
        returner += `</${gradientType}Gradient><rect ${rectangleDimension} fill="url(#g)" /></svg>`;

        returner = encodeURIComponent(returner);

        returner = `data:image/svg+xml,${returner}`;
        return new URL(new Quoted(`'${returner}'`, returner, false, this.index, this.currentFileInfo), this.index, this.currentFileInfo);
    }};
};
