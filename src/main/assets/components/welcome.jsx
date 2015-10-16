import React from 'react'
import { Link } from 'react-router'

export default class Welcome extends React.Component {
  render() {
    return (
    	<div className="welcome">
    		Traffic Lights Control
    		<div className="panel">
    			[ <Link to="/lights">Dashboard</Link> ]
    		</div>
    	</div>
	);
  }
}