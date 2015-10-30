import TrafficSystemStore from '../stores/trafficSystemStore.js'

export class LightStateListenerMixin {

  componentDidMount(){
    let {systemId,compId} = this.props
    TrafficSystemStore.registerListener(`${systemId}_${compId}`, this.updateLightState.bind(this))
  }

  componentWillUnmount(){
    let {systemId,compId} = this.props
    TrafficSystemStore.unregisterListener(`${systemId}_${compId}`)
  }

  updateLightState(newState){
    if(newState && newState !== this.state.lightState) this.setState({lightState: newState})
  }

} 