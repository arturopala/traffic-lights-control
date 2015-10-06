const WS = window['MozWebSocket'] ? MozWebSocket : WebSocket

export default function listenToWebsocket(path, callback){

  let socket = new WS("ws://"+window.location.host+path)

  socket.onmessage = function (event) {
    if(typeof event.data !== undefined){
      callback(event.data)
    }
  }

  return socket;
}

       