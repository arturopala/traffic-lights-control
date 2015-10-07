const websocket = window['MozWebSocket'] ? MozWebSocket : WebSocket

export default function WS(path, callback){

  let socket = new websocket("ws://"+window.location.host+path)

  socket.onmessage = function (event) {
    if(typeof event.data !== undefined){
      callback(event.data)
    }
  }

  return socket;
}

       