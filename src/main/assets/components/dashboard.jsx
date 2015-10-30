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
    this.fetchLayout(this.props)
  }

  componentWillReceiveProps(nextProps){
    this.fetchLayout(nextProps)
  }

  fetchLayout(props){
    let {systemId,compId} = props.params
    let {layout} = props
    if(!layout) {
      TrafficSystemStore.getLayout(systemId).then(layout => {
        let component = this.findComponent(compId,layout)
        this.setState({layout: component})
      })
    } else {
      let component = this.findComponent(compId,layout)
      this.setState({layout: component})
    }
  }

  findComponent(compId, layout){
    let component = layout
    if(compId){
      if(component && component.id && component.id === compId) {
        return component;
      }
      else if(component && component.member) return this.findComponent(compId, component.member);
      else if(component && component.members) {
        let res = component.members.map(member => this.findComponent(compId, member)).filter( e => e !== undefined)
        if(res && res.length>0) return res[0]; else return undefined;
      } else return undefined;
    }
    
    return component
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
      <div key={`${systemId}_${compId}`} className="dashboard">
        <span className="label"><Link to={'/'}>Traffic Lights Control</Link><Link to={`/${systemId}`}> / <b>{systemId}</b></Link>{compId?(<Link to={`/${systemId}/${compId}`}>{' / '+compId}</Link>):''}</span>
    		<div className="panel">
        {generate(this.state.layout)}
	     </div>
      </div>
	);
  }

}

export default Dashboard