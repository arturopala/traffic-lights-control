import Redux from 'redux'
import merge from 'deepmerge'
import { combineReducers, createStore } from 'redux';
import { LightStatusUpdate, RegisterLightStateListener, UnregisterLightStateListener, SetApiError } from './actions.js'

const initialState = {
	lightStateMap: {},
	listeners: {}
}

const reducer = function(state = initialState, action) {
  switch (action.type) {
	  case LightStatusUpdate:
	  	Object.getOwnPropertyNames(action.lightStateMap).map( lightId => {
		    let listener = state.listeners[lightId]
		    if(listener) listener(action.lightStateMap[lightId])
		})
	    return merge(state, {lightStateMap: action.lightStateMap})
	  case RegisterLightStateListener:
	    action.callback(state.lightStateMap[action.lightId])
	  	return merge(state, {listeners:{[action.lightId]:action.callback}})
	  case UnregisterLightStateListener:
	  	return merge(state, {listeners:{[action.lightId]:undefined}})
	  default:
	    return state;
  }
}

const Store = createStore(reducer)

export default Store