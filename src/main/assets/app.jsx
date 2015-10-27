import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'
import { Provider } from 'react-redux';

import Page from './components/page.jsx';
import Dashboard from './components/dashboard.jsx'
import LightBoard from './components/lightboard.jsx'
import Welcome from './components/welcome.jsx'

import Store from './store.js'
import {updateLightStatus, setLightStateMap, setLightStatusError} from './actions.js'
import WS from './websocket.js'
import Fetch from './fetch.js'

const routes = <Router history={createBrowserHistory()}>
    		<Route path="/" component={Page}>
	    		<IndexRoute component={Welcome} />
	    		<Route path="/:systemId" component={Dashboard} />
	    		<Route path="/:systemId/:lightId" component={LightBoard} />
		    </Route>
		</Router>

class App extends React.Component {
  render() {
    return (
    	<Provider store={Store}>{routes}</Provider>
	);
  }
}

const notifyStore = function(message){
	if(message){
    	const [lightId, lightState] = message.split(':')
    	if(lightId && lightState){
			Store.dispatch(updateLightStatus(lightId, lightState))
		}
	}
}

const receiveAllLightsStateResponse = function(response){
	if (response.status >= 200 && response.status < 300) {
      response.json().then(state => Store.dispatch(setLightStateMap(state.report)))
    } else {
      Store.dispatch(setApiError(response.status, response.statusText))
    }
}

Fetch(`/api/lights`, receiveAllLightsStateResponse.bind(this))

WS('/ws/lights', notifyStore)

export default App