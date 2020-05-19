var app = angular.module( 'app', ['react'] );

app.controller( 'mainCtrl', function( $scope ) {

  function generateRows( ) {
    var rows = [];
    for ( var i = 0; i < 5000; i++ ) {
      var d = new Date();
      rows.push( [
        'First Name ' + i,
        'Last Name ' + i,
        'name' + i + '@domain.com',
        '@name' + i,
        i + '-' + i,
        d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds()
      ] );
    }
    return rows;
  }

  $scope.table = {
    cols: [ 'First Name', 'Last Name', 'Email', 'Twitter', 'Id', 'Modified' ],
    rows: generateRows()
  };

  $scope.regenerate = function() {
    $scope.table.rows = generateRows( );
  };

} );

app.value( "MyTable", React.createClass( {

  propTypes : {
    table: React.PropTypes.object.isRequired
  },

  getDefaultProps: function() {
    return { table: { rows: [], cols: [] } };
  },

  render: function() {
    var cols = this.props.table.cols.map( function( col, i ) {
      return React.DOM.th( { key: i }, col );
    } );
    var header = React.DOM.thead( null, React.DOM.tr( {key:'header'}, cols ) );

    var body = React.DOM.tbody( null, this.props.table.rows.map( function( row, i ) {
      return React.DOM.tr( { key: i }, row.map( function( cell, j ) {
        return React.DOM.td( { key: j }, cell );
      } ) );
    } ) );

    return React.DOM.table( {key:'body', className:'pure-table'}, [header, body] );
  }
} ) );

app.directive( 'myTable', function( reactDirective ) {
  return reactDirective( 'MyTable' );
} );