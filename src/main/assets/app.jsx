import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'
import { Provider } from 'react-redux';

import Page from './components/page.jsx';
import Dashboard from './components/dashboard.jsx'
import LightBoard from './components/lightboard.jsx'
import Welcome from './components/welcome.jsx'

import Store from './store.js'
import WS from './websocket.js'
import { updateLightStatus } from './actions.js'

const routes = () => {
	return (
		<Router history={createBrowserHistory()}>
    		<Route path="/" component={Page}>
	    		<IndexRoute component={Welcome} />
	    		<Route path="/lights" component={Dashboard} />
	    		<Route path="/lights/:lightId" component={LightBoard} />
		    </Route>
		</Router>
	);
}

class App extends React.Component {

  constructor() {
  	super()
  	this.socket = undefined
  }

  componentWillMount(){
  	this.socket = WS('/ws/lights', (message) => {
		let [lightId, lightState] = message.split(':')
		if(lightId && lightState){
			Store.dispatch(updateLightStatus(lightId, lightState))
		}
	})
  }

  componentDidUnmount(){
  	if(this.socket) this.socket.close()
    this.socket = undefined
  }

  render() {
    return (
    	<Provider store={Store}>{routes}</Provider>
	);
  }
}

export default App