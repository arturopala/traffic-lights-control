import React from 'react';
import './light.css'
import Store from '../store.js'
import {registerLightStateListener, unregisterLightStateListener} from '../actions.js'

export default class Light extends React.Component {

  constructor(){
    super()
    this.state = {lightState: 'R'}
  }

  componentDidMount(){
	let {systemId,compId} = this.props
  	Store.dispatch(registerLightStateListener(`${systemId}_${compId}`, this.updateLightState.bind(this)));
  }

  componentWillUnmount(){
	let {systemId,compId} = this.props
  	Store.dispatch(unregisterLightStateListener(`${systemId}_${compId}`));
  }

  updateLightState(newState){
  	if(newState) this.setState({lightState: newState})
  }

  render() {
  	let {systemId,compId} = this.props
    return (
    	<span className="light">
    		<span className="label">{compId}</span>
	    	<span className={"panel state"+this.state.lightState}>
	    		<span className="box boxRed"></span>
	    		<span className="box boxYellow"></span>
	    		<span className="box boxGreen"></span>
	    	</span>
    	</span>
	);
  }
}