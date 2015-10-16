import React from 'react';
import './light.css'

export default class Light extends React.Component {
  render() {
    return (
    	<span className="light">
    		<span className="label">{this.props.lightId}</span>
	    	<span className={"panel state"+this.props.lightState}>
	    		<span className="box boxRed"></span>
	    		<span className="box boxYellow"></span>
	    		<span className="box boxGreen"></span>
	    	</span>
    	</span>
	);
  }
}