import React from 'react'
import Actions from '../actions.js'
import Store from '../store.js'
import Light from './light.jsx'

export default class Dashboard extends React.Component {

  constructor(props) {
    super(props)
    this.state = {}
  }

  componentWillMount() {
    this.unsubscribe = Store.listen(this.onStatusUpdate.bind(this))
    Actions.WatchStatus()
  }

  componentDidUnmount() {
    Actions.StopStatus()
    this.unsubscribe()
  }

  onStatusUpdate({lightId, lightState}){
  	this.setState({
		[lightId]: lightState
  	})
  }

  render() {
    return (
    	<div className="dashboard">
    	{	
    		Object.getOwnPropertyNames(this.state).map((lightId) => {
    			return <Light key={lightId} lightId={lightId} lightState={this.state[lightId]}/>;
			})
    	}
    	</div>
	);
  }
}