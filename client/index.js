const wsrpc=require('express-ws-rpc')
const WebSocket = require('simple-websocket')

let ws=new WebSocket(getWsAbsoluteUrl('/ws'))
function getWsAbsoluteUrl(relative) {
    var loc = window.location;
    var proto = loc.protocol === "https:" ? "wss://" : "ws://";
    var port = loc.port || (loc.protocol === "https:" ? 443 : 80);
    return proto + loc.hostname + ":" + port + relative;
}
$(()=>{
	wsrpc(ws)
	ws.on('update_pic',(data,cb)=>{
		console.log('got picture')
    try{
		    $('#mypic').attr('src',data)
        cb(null,true)
    }catch(e){
        cb(e,false)
    }
	})
	ws.on('error',(msg)=>{
  	 console.warn(msg)
	})
	ws.on('connect',()=>{
		window.ws=ws
	console.log('wesocket open')
	})
	ws.on('close',()=>{
		delete window.ws
	console.log('websocket close')
	})
	}
)
