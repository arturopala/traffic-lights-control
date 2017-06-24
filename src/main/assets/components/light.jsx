import React from 'react'
import { Link } from 'react-router'
import './light.css'
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
    	<span key={`${systemId}_${compId}`} className="light">
    		<Link to={"/"+systemId+"/"+compId}><span className="label">{compId}</span></Link>
	    	<span className={"panel light_state"+this.state.lightState}>
	    		<span className="box boxRed"></span>
	    		<span className="box boxYellow"></span>
	    		<span className="box boxGreen"></span>
	    	</span>
    	</span>
	);
  }
}