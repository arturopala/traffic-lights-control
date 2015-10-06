import React from 'react'
import Actions from '../actions.js'
import Store from '../store.js'

export default class Dashboard extends React.Component {

  componentWillMount() {
    this.unsubscribe = Store.listen(this.onStatusUpdate)
    Actions.WatchStatus()
  }

  componentDidUnmount() {
    Actions.StopStatus()
    this.unsubscribe()
  }

  onStatusUpdate(data){
	console.log(data)
  }

  render() {
    return (
    	<div className="dashboard">
    		Dashboard
    	</div>
	);
  }
}