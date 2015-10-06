import React from 'react';

export default class Light extends React.Component {
  render() {
    return (
    	<div className="light">
    		Light # {this.props.lightId} is {this.props.lightState}
    	</div>
	);
  }
}