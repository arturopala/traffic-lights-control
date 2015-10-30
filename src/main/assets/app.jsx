import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'
import { Provider } from 'react-redux';

import Page from './components/page.jsx';
import Dashboard from './components/dashboard.jsx'
import LightBoard from './components/lightboard.jsx'
import Welcome from './components/welcome.jsx'

import TrafficSystemStore from './stores/trafficSystemStore.js'

TrafficSystemStore.start()

const routes = <Router history={createBrowserHistory()}>
    		<Route path="/" component={Page}>
	    		<IndexRoute component={Welcome} />
	    		<Route path="/:systemId" component={Dashboard} />
	    		<Route path="/:systemId/:lightId" component={Dashboard} />
		    </Route>
		</Router>

class App extends React.Component {
  render() {
    return routes;
  }
}

export default App