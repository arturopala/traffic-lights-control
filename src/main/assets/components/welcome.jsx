import React from 'react'
import { Link } from 'react-router'
import './welcome.css'

export default class Welcome extends React.Component {
  render() {
    return (
    	<div className="welcome">
    		Traffic Lights Control
    		<div className="panel">
    			[ <Link to="/demo">Dashboard</Link> ]
    		</div>
    	</div>
	);
  }
}