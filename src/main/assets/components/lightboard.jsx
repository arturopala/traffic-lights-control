import React from 'react'
import { Link } from 'react-router'
import { connect } from 'react-redux'

import Light from './light.jsx'

class LightBoard extends React.Component {

  constructor(){
  	super()
  	this.state = {recent:[]}
  }

  componentWillReceiveProps(nextProps){
  	const currLightId = this.props.params.lightId || this.props.lightId
  	const newLightId = nextProps.params.lightId || nextProps.lightId
  	const { lightStateMap } = this.props
  	if(currLightId !== newLightId){
  		this.setState({lightState:undefined})
  	}
  	let newLightState = lightStateMap[newLightId]
  	if(newLightState && newLightState!==this.state.lightState){
  		this.state.recent.push(newLightState)
  		this.setState({lightState:newLightState})
  	}
  }

  shouldComponentUpdate(nextProps, nextState){
  	const currLightId = this.props.params.lightId || this.props.lightId
  	const newLightId = nextProps.params.lightId || nextProps.lightId
  	return (currLightId !== newLightId) || (nextState.lightState !== this.state.lightState)
  }

  render() {
  	const lightId = this.props.params.lightId || this.props.lightId
    return (
    	<div>
	    	<div><Link to="/lights">Dashboard</Link></div>
	    	<div>
	    		<Light lightId={lightId} lightState={this.state.lightState}/>
	    		<div>{this.state.recent}</div>
	    	</div>
    	</div>
	);
  }
}

const selector = (state) => {
	return {
		lightStateMap: state.lightStateMap
	}
}

export default connect(selector)(LightBoard)