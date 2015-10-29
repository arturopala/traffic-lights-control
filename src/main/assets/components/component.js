import React from 'react';
import Store from '../store.js'
import {registerLightStateListener, unregisterLightStateListener} from '../actions.js'

export class LightStateListenerMixin {

  componentDidMount(){
    let {systemId,compId} = this.props
    Store.dispatch(registerLightStateListener(`${systemId}_${compId}`, this.updateLightState.bind(this)));
  }

  componentWillUnmount(){
    let {systemId,compId} = this.props
    Store.dispatch(unregisterLightStateListener(`${systemId}_${compId}`));
  }

  updateLightState(newState){
    if(newState) this.setState({lightState: newState})
  }

} 