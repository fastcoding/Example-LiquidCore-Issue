var express = require('express');
var router = express.Router();
var wsrpc = require('express-ws-rpc')
var work = require('../work')

router.ws('/', function(ws,req) {
   // this function gets called on each connection
    // extend ws to decode messages
    wsrpc(ws);
    ws.on('close', () => {
        work.del_ws(ws)
        console.log('websocket closed,remaining :', work.ws_count())
    })
    work.add_ws(ws)
    console.log('new websocket created - ', work.ws_count())

});

module.exports = router;
