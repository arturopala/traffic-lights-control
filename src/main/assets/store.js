import Redux from 'redux'
import merge from 'deepmerge'
import { combineReducers, createStore } from 'redux';
import { LightStatusUpdate } from './actions.js'

const initialState = {
	lightStateMap: {}
}

const reducer = function(state = initialState, action) {
  switch (action.type) {
	  case LightStatusUpdate:
	    return merge(state, {lightStateMap: action.lightStateMap});
	  default:
	    return state;
  }
}

const Store = createStore(reducer)

export default Store