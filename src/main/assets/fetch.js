export default function Fetch(path, onmessage, options, onerror){
  let host = window.location.host
  if (host === "localhost:8081") host = "localhost:8080"
  let url = `${window.location.protocol}//${host}${path}`
  fetch(url, options).then(onmessage, onerror)
}