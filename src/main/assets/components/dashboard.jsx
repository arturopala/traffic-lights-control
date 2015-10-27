import React from 'react'
import { Link } from 'react-router'
import Fetch from '../fetch.js'
import merge from 'deepmerge'
import mixin from '../mixin.js'
import Light from './light.jsx'
import Sequence from './sequence.jsx'
import Group from './group.jsx'

class Dashboard extends React.Component {

  constructor(){
    super()
    this.state = {}
  }

  componentDidMount(){
    Fetch(`/api/layouts/${this.props.params.systemId}`, this.receiveLayoutResponse.bind(this))
  }

  receiveLayoutResponse(response){
    if (response.status >= 200 && response.status < 300) {
      response.json().then(layout => this.setState({layout: layout}))
    } else {
      this.setState({error: response.statusText})
    }
  }

  render() {
    let systemId = this.props.params.systemId
    let generate = function(comp) {
      if(comp) {
        switch(comp['type']){
          case "sequence":
            return <Sequence key={comp.id} compId={comp.id} systemId={systemId} members={comp.members} generate={generate}/>
          case "group":
            return <Group key={comp.id} compId={comp.id} systemId={systemId} members={comp.members} generate={generate}/>
          case "light":
            return <Light key={comp.id} compId={comp.id} systemId={systemId}/>
        }
      } else {
        return <div/>
      }
    }
    return (
      <div className="dashboard">
        <span className="label">Traffic Lights Control / <b>{this.props.params.systemId}</b></span>
    		<div className="panel">
        {generate(this.state.layout)}
	     </div>
      </div>
	);
  }

}

export default Dashboard