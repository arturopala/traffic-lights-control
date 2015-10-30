import WS from '../websocket.js'
import Fetch from '../fetch.js'

class TrafficSystemStore {

	constructor(){
		this.state = {
			layoutList: [],
			lightStateMap: {},
			listeners: {},
			layouts: {}
		}
	}

	start(){
		console.log('TrafficSystemStore starting ..')
		Fetch('/api/layouts', this.receiveJson(this.setLayoutList.bind(this)))
		Fetch('/api/lights', this.receiveJson(this.setInitialLightsState.bind(this)), (obj) => obj.report)
		WS('/ws/lights', this.receiveWsMessage.bind(this))
	}

	setLayoutList(layoutList){
		this.state.layoutList = layoutList
	}

	getLayout(systemId){
		let layout = this.state.layouts[systemId]
		if(layout) return new Promise((resolve,reject) => resolve(layout))
		else return Fetch(`/api/layouts/${systemId}`).then((response) => {
			if (response.status >= 200 && response.status < 300) {
				let layout = response.json()
				if(layout){
					this.state.layouts[systemId] = layout
					return layout
				}
			} else {
				return this.handleError(response)
			}
		})
	}

	setInitialLightsState(lightStateMap){
		//console.log(lightStateMap)
		this.state.lightStateMap = lightStateMap
		Object.getOwnPropertyNames(lightStateMap).map( lightId => {
			let listener = this.state.listeners[lightId]
			if(listener) listener(lightStateMap[lightId])
		})
	}

	updateLightState(lightId, lightState){
		//console.log(lightId, lightState)
		this.state.lightStateMap[lightId] = lightState
		let listener = this.state.listeners[lightId]
		if(listener) listener(lightState)
	}

	registerListener(lightId, listener){
		//console.log(lightId, listener)
		this.state.listeners[lightId] = listener
	}

	unregisterListener(lightId){
		//console.log(lightId)
		this.state.listeners[lightId] = undefined
	}

	receiveJson(onSuccess, transform = function(a){return a}){ return (response) => {
		//console.log(response)
		if (response.status >= 200 && response.status < 300) {
	      return response.json().then(obj => onSuccess(transform(obj)))
	    } else {
	      return this.handleError(response)
   		}
	}}

	receiveWsMessage(message){
		if(message){
	    	let [lightId, lightState] = message.split(':')
	    	if(lightId && lightState){
				this.updateLightState(lightId, lightState)
			}
		}
	}

	handleError(response){
		console.log('Error: '+response.status+' '+response.statusText)
	}

}

const store = new TrafficSystemStore

export default store