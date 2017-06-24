import TrafficSystemStore from '../stores/trafficSystemStore.js'

export class LightStateListenerMixin {

  componentDidMount(){
    let {systemId,compId} = this.props
    TrafficSystemStore.registerListener(`${systemId}_${compId}`, this.updateLightState.bind(this))
  }

  componentWillReceiveProps(nextProps){
    let {systemId,compId} = this.props
    let nextSystemId = nextProps.systemId
    let nextCompId = nextProps.compId
    if(systemId !== nextSystemId || compId !== nextCompId){
      TrafficSystemStore.unregisterListener(`${systemId}_${compId}`)
      TrafficSystemStore.registerListener(`${nextSystemId}_${nextCompId}`, this.updateLightState.bind(this))
    }
  }

  componentWillUnmount(){
    let {systemId,compId} = this.props
    TrafficSystemStore.unregisterListener(`${systemId}_${compId}`)
  }

  updateLightState(newState){
    if(newState && newState !== this.state.lightState) this.setState({lightState: newState})
  }

} 