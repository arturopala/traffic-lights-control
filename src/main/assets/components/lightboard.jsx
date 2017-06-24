import React from 'react'
import { Link } from 'react-router'
import WS , { WebSocketListenerMixin } from '../websocket.js'
import merge from 'deepmerge'
import mixin from '../mixin.js'
import Light from './light.jsx'
import './lightboard.css'

class LightBoard extends React.Component {

  constructor(){
    super()
    this.state = {}
  }

  componentDidMount(){
    Fetch(`/api/layouts/${this.props.params.systemId}`/`${this.props.params.compId}`, this.receiveLayoutResponse.bind(this))
  }

  receiveLayoutResponse(response){
    if (response.status >= 200 && response.status < 300) {
      response.json().then(layout => this.setState({layout: layout}))
    } else {
      this.setState({error: response.statusText})
    }
  }

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