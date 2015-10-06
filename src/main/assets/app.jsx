import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'

import Page from './components/page.jsx';
import Dashboard from './components/dashboard.jsx'
import Light from './components/light.jsx'
import Welcome from './components/welcome.jsx'

export default class App extends React.Component {
  render() {
    return (
		<Router history={createBrowserHistory()}>
    		<Route path="/" component={Page}>
	    		<IndexRoute component={Welcome} />
	    		<Route path="/lights" component={Dashboard} />
	    		<Route path="/lights/:id" component={Light} />
		    </Route>
		</Router>
	);
  }
}