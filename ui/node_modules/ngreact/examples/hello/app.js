var app = angular.module( 'app', ['react'] );

app.controller( 'mainCtrl', function( $scope ) {
  $scope.person = { fname: 'Clark', lname: 'Kent' };
} );

var Hello = React.createClass( {
  propTypes: {
    fname: React.PropTypes.string.isRequired,
    lname: React.PropTypes.string.isRequired
  },

  render: function() {
    return React.DOM.span( null,
      'Hello ' + this.props.fname + ' ' + this.props.lname
    );
  }
} );

app.value( "Hello", Hello );

app.directive( 'hello', function( reactDirective ) {
  return reactDirective( Hello );
} );