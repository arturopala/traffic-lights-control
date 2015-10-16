import React from 'react'
import { Link } from 'react-router'
import WS , { WebSocketListenerMixin } from '../websocket.js'
import merge from 'deepmerge'
import mixin from '../mixin.js'

import Light from './light.jsx'

const initialState = {
  lightId: undefined,
  lightState: undefined,
  socket: undefined
}

class LightBoard extends mixin(React.Component, WebSocketListenerMixin) {

  constructor() {
    super()
    this.state = initialState
  }

  webSocketPath(){
    const lightId = this.props.params.lightId || this.props.lightId
    if(lightId){
      return `/ws/lights/${lightId}`
    } else {
      throw new Error("Some lightId expected")
    }
  }

  receiveMessage(message){
    if(message){
      this.setState({ lightState: message})
    }
  }

  render() {
  	const lightId = this.props.params.lightId || this.props.lightId
    return (
    	<div className="lightboard">
        <div className="panel">
	    	  <Link to="/lights"><Light lightId={lightId} lightState={this.state.lightState}/></Link>
        </div>
    	</div>
	);
  }
}

export default LightBoard