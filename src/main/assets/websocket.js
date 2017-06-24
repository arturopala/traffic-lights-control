const websocket = window['MozWebSocket'] ? MozWebSocket : WebSocket

export default function WS(path, onmessage, onerror, onopen, onclose){

  let host = window.location.host

  if (host === "localhost:8081") host = "localhost:8080"

  let socket = new websocket("ws://"+host+path)

  if(onmessage) socket.onmessage = function (event){
    if(typeof event.data !== undefined){
      onmessage(event.data)
    }
  }

  if(onerror) socket.onerror = function(event){
  	onerror(event)
  }

  if(onerror) socket.onopen = function(event){
  	onopen(event)
  }

  if(onerror) socket.onclose = function(event){
  	onclose(event)
  }

  return socket;
}

export class WebSocketListenerMixin {

  bind(f){
  	if(f) return f.bind(this)
  }

  listenForUpdates(){
  	let path = this.webSocketPath()
  	if(path){
  		let { receiveMessage, handleWebSocketError, handleWebSocketOpen, handleWebSocketClosed } = this
    	let socket = WS(path, this.bind(receiveMessage), this.bind(handleWebSocketError), this.bind(handleWebSocketOpen), this.bind(handleWebSocketClosed))
    	this.socket = socket
    } else {
    	throw new Error("Some websocket path expected")
    }
  }

  cancelListening(){
    if(this.socket) this.socket.close()
  }

  componentDidMount(){
    this.listenForUpdates()
  }

  componentWillUnmount(){
    this.cancelListening()
  }

} 

       