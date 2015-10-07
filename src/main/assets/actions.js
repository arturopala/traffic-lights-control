export const LightStatusUpdate = 'LightStatusUpdate'

export function updateLightStatus(lightId,lightState){ 
	return {
		type: LightStatusUpdate, 
		lightStateMap: {
			[lightId]: lightState
		}
	}
}