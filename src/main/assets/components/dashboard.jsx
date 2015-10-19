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
  	return `/ws/lights/${this.props.params.systemId}`
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
    return (
      <div className="dashboard">
    		<div className="panel">

    		{Object.getOwnPropertyNames(lightStateMap).map(
    			id => {
            const [systemId, lightId] = id.split('_');
            return <Link key={id} to={`/${systemId}/${lightId}`}><Light systemId={systemId} lightId={lightId} lightState={lightStateMap[id]}/></Link>;
          }
    		)}
	     </div>
      </div>
	);
  }

}

Dashboard.propTypes = { lightStateMap: React.PropTypes.object.isRequired };
Dashboard.defaultProps = { lightStateMap: {} };

export default Dashboard