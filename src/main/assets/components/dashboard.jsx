import React from 'react'
import { Link } from 'react-router'
import merge from 'deepmerge'
import mixin from '../mixin.js'

import Light from './light.jsx'
import Sequence from './sequence.jsx'
import Group from './group.jsx'

import TrafficSystemStore from '../stores/trafficSystemStore.js'

import './dashboard.css'

class Dashboard extends React.Component {

  constructor(){
    super()
    this.state = {}
  }

  componentDidMount(){
    this.fetchLayout()
  }

  fetchLayout(){
    let {systemId,compId} = this.props.params
    let {layout} = this.props
    if(!layout) {
      TrafficSystemStore.getLayout(systemId).then(layout => {
        this.setState({layout: layout})
      })
    } else setState({layout: layout})
  }

  render() {
    let {systemId,compId} = this.props.params
    let generate = function(comp) {
      if(comp) {
        switch(comp['type']){
          case "sequence":
            return <Sequence key={comp.id} compId={comp.id} systemId={systemId} members={comp.members} generate={generate}/>
          case "group":
            return <Group key={comp.id} compId={comp.id} systemId={systemId} members={comp.members} generate={generate}/>
          case "offset":
            return generate(comp.member)
          case "light":
            return <Light key={comp.id} compId={comp.id} systemId={systemId}/>
        }
      } else {
        return <div/>
      }
    }
    return (
      <div className="dashboard">
        <span className="label">Traffic Lights Control / <b>{systemId}</b></span>
    		<div className="panel">
        {generate(this.state.layout)}
	     </div>
      </div>
	);
  }

}

export default Dashboard