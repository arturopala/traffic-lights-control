import React from 'react'
import { Link } from 'react-router'
import WS , { WebSocketListenerMixin } from '../websocket.js'
import merge from 'deepmerge'
import mixin from '../mixin.js'
import Light from './light.jsx'
import './lightboard.css'

class LightBoard extends React.Component {

  render() {
    return (
    	<div className="lightboard">
        <div className="panel">
	    	 
        </div>
    	</div>
	);
  }
}

export default LightBoard