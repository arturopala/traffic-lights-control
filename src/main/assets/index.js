import "babel-core/polyfill"
import React from 'react'
import App from './app.jsx'

let index = function (appCreator) {
    React.render(<App/>, document.getElementById('app'))
}

index()