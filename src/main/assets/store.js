import Reflux from 'reflux'
import Actions from './actions.js'
import listenToWebsocket from './websocket.js'

const Store = Reflux.createStore({

    listenables: Actions,

    _state: {},

    getInitialState: function(){
		return this._state
	},

    init: function() {},

	handleLightStatusUpdate: function (message) {
	  	let [ id, state ] = message.split(':')
      	this.trigger({ id, state })
	},

    onWatchStatus: function() {
    	this._state.socket = listenToWebsocket('/ws/lights', this.handleLightStatusUpdate)
    },

    onStopStatus: function() {
    	this._state.socket.close()
    }
})

export default Store

