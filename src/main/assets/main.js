// Polyfill
import "babel-core/polyfill";

import React from 'react';
import App from './app.jsx';

main();

function main() {
    React.render(<App/>, document.getElementById('app'));
}