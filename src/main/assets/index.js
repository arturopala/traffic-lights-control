import "babel-core/polyfill"
import React from 'react'
import ReactDOM from 'react-dom'
import App from './app.jsx'

let index = function (appCreator) {
    ReactDOM.render(<App/>, document.getElementById('app'))
}

index()