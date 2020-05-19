var Hello = React.createClass( {
  propTypes: {
    fname: React.PropTypes.string.isRequired,
    lname: React.PropTypes.string.isRequired
  },

  render: function() {
    return <span>Hello {this.props.fname} {this.props.lname}</span>;
  }
} );