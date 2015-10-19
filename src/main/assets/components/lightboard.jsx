import React from 'react'
import { Link } from 'react-router'
import WS , { WebSocketListenerMixin } from '../websocket.js'
import merge from 'deepmerge'
import mixin from '../mixin.js'

import Light from './light.jsx'

const initialState = {
  lightStateMap: {},
  socket: undefined
}

class LightBoard extends mixin(React.Component, WebSocketListenerMixin) {

  constructor() {
    super()
    this.state = initialState
  }

  webSocketPath(){
    const systemId = this.props.params.systemId || this.props.systemId
    const lightId = this.props.params.lightId || this.props.lightId
    if(lightId){
      return `/ws/lights/${systemId}/${lightId}`
    } else {
      throw new Error("Some lightId expected")
    }
  }

  receiveMessage(message){
    if(message){
      const [id, lightState] = message.split(':')
      if(id && lightState){
        this.setState(merge(this.state, {lightStateMap:{ [id]:lightState }}))
      }
    }
  }

  render() {
    const { lightStateMap } = this.state
    const systemId = this.props.params.systemId || this.props.systemId
  	const lightId = this.props.params.lightId || this.props.lightId
    return (
    	<div className="lightboard">
        <div className="panel">
	    	  <Link to={`/${systemId}`}><Light systemId={systemId} lightId={lightId} lightState={lightStateMap[systemId+'_'+lightId]}/></Link>
        </div>
    	</div>
	);
  }
}

export default LightBoard