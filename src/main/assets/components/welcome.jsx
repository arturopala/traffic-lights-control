import React from 'react'
import { listenTo } from 'reflux'
import Actions from '../actions.js'
import Store from '../store.js'

export default class Welcome extends React.Component {

  componentWillMount() {
    this.unsubscribe = listenTo(Store, () => {})
  }

  componentDidUnmount() {
    this.unsubscribe()
  }

  render() {
    return (
    	<div className="welcome">
    		Welcome
    	</div>
	);
  }
}