// Polyfill
import "babel-core/polyfill";

import React from 'react';
import Header from './jsx/component.jsx';

main();

function main() {
    React.render(<Header/>, document.getElementById('app'));
}