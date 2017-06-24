import React from 'react'
import { Link } from 'react-router'
import './welcome.css'

import TrafficSystemStore from '../stores/trafficSystemStore.js'

export default class Welcome extends React.Component {

  constructor(){
    super()
    this.state = {layouts: []}
  }

  componentDidMount(){
    this.fetchLayoutsList(this.props)
  }

  fetchLayoutsList(props){
     TrafficSystemStore.getLayoutList().then(layouts => {
      
      this.setState({layouts: layouts})
    })
  }

  render() {
    let layouts = this.state.layouts
    console.log(layouts)
    return (
    	<div className="welcome">
    		Traffic Lights Control
    		<div className="panel">
          {layouts.map( layoutName =>  <span key={layoutName}>[ <Link to={`/${layoutName}`}>{layoutName}</Link> ]</span> )}
    		</div>
    	</div>
	);
  }
}