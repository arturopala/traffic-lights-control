import React from 'react'
import { Router, Route, Link, IndexRoute, Redirect } from 'react-router'
import createBrowserHistory from 'history/lib/createBrowserHistory'

import Page from './page.jsx';
import Dashboard from './dashboard.jsx'
import Light from './light.jsx'
import Welcome from './welcome.jsx'

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