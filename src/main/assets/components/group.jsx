import React from 'react';

export default class Group extends React.Component {
  render() {
  	let {compId, systemId, members, generate} = this.props
    return <span className="group">
    		<span className="label">{compId}</span>
	    	{members.map(generate)}
    	</span>
  }
}