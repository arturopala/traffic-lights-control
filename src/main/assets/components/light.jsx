import React from 'react'
import './light.css'
import Store from '../store.js'
import {registerLightStateListener, unregisterLightStateListener} from '../actions.js'
import {LightStateListenerMixin} from './component.js'
import mixin from '../mixin.js'

export default class Light extends mixin(React.Component, LightStateListenerMixin) {

  constructor(){
    super()
    this.state = {lightState: 'R'}
  }

  render() {
  	let {systemId,compId} = this.props
    return (
    	<span className="light">
    		<span className="label">{compId}</span>
	    	<span className={"panel light_state"+this.state.lightState}>
	    		<span className="box boxRed"></span>
	    		<span className="box boxYellow"></span>
	    		<span className="box boxGreen"></span>
	    	</span>
    	</span>
	);
  }
}