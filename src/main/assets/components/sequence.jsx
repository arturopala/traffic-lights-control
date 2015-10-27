import React from 'react';

export default class Sequence extends React.Component {
  render() {
  	let {compId, systemId, members, generate} = this.props
    return <span className="sequence">
    		<span className="label">{compId}</span>
	    	{members.map(generate)}
    	</span>
  }
}