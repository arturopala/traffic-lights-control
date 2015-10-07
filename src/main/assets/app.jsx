import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'
import { Provider } from 'react-redux';

import Page from './components/page.jsx';
import Dashboard from './components/dashboard.jsx'
import LightBoard from './components/lightboard.jsx'
import Welcome from './components/welcome.jsx'

import Store from './store.js'

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
  render() {
    return (
    	<Provider store={Store}>{routes}</Provider>
	);
  }
}

export default App