import React from 'react'
import {LightStateListenerMixin} from './component.js'
import mixin from '../mixin.js'
import './group.css'

export default class Group extends mixin(React.Component, LightStateListenerMixin) {

  constructor(){
    super()
    this.state = {lightState: 'R'}
  }

  render() {
  	let {compId, systemId, members, generate} = this.props
    return <span className={"group group_state"+this.state.lightState}>
    		<span className="label">{compId}</span>
	    	{members.map(generate)}
    	</span>
  }
}