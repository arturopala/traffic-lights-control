import React from 'react'
import { Link } from 'react-router'
import { connect } from 'react-redux'

import Light from './light.jsx'
import Store from '../store.js'

class Dashboard extends React.Component {
  render() {
  	const { lightStateMap } = this.props
    return (
		<div className="dashboard">
		{Object.getOwnPropertyNames(lightStateMap).map(
			lightId => <Link key={lightId} to={`/lights/${lightId}`}><Light lightId={lightId} lightState={lightStateMap[lightId]}/></Link>
		)}
	    </div>
	);
  }

}

Dashboard.propTypes = { lightStateMap: React.PropTypes.object.isRequired };
Dashboard.defaultProps = { lightStateMap: {} };

const selector = (state) => {
	return {
		lightStateMap: state.lightStateMap
	}
}

export default connect(selector)(Dashboard)