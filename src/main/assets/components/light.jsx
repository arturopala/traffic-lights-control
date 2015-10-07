import React from 'react';
import './light.css'

export default class Light extends React.Component {
  render() {
    return (
    	<span className='light'>
    		Light # {this.props.lightId} is {this.props.lightState}
    	</span>
	);
  }
}