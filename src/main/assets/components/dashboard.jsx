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

class Dashboard extends mixin(React.Component, WebSocketListenerMixin) {

  constructor() {
  	super()
  	this.state = initialState
  }

  webSocketPath(){
  	return '/ws/lights'
  }

  receiveMessage(message){
	const [lightId, lightState] = message.split(':')
	if(lightId && lightState){
		this.setState(merge(this.state, {lightStateMap:{ [lightId]:lightState }}))
	}
  }

  render() {
  	const { lightStateMap } = this.state
    return (
		<div className="dashboard">
		{Object.getOwnPropertyNames(lightStateMap).map(
			lightId => <Link key={lightId} to={`/lights/${lightId}`}><Light lightId={lightId} lightState={lightStateMap[lightId]}/></Link>
		)}
	    </div>
	);
  }

}

Dashboard.propTypes = { lightStateMap: React.PropTypes.object.isRequired };
Dashboard.defaultProps = { lightStateMap: {} };

export default Dashboard