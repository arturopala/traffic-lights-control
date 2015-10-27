export const LightStatusUpdate = 'LightStatusUpdate'
export const SetApiError = 'SetApiError'
export const RegisterLightStateListener = 'RegisterLightStateListener'
export const UnregisterLightStateListener = 'UnregisterLightStateListener'

export function updateLightStatus(lightId,lightState){ 
	return {
		type: LightStatusUpdate, 
		lightStateMap: {
			[lightId]: lightState
		}
	}
}

export function setLightStateMap(lightStateMap){ 
	return {
		type: LightStatusUpdate, 
		lightStateMap: lightStateMap
	}
}

export function setApiError(status, statusText){ 
	return {
		type: SetApiError, 
		status: status,
		statusText: statusText
	}
}

export function registerLightStateListener(lightId, callback){ 
	return {
		type: RegisterLightStateListener, 
		lightId: lightId,
		callback: callback
	}
}

export function unregisterLightStateListener(lightId){ 
	return {
		type: UnregisterLightStateListener, 
		lightId: lightId
	}
}