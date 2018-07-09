const _=require('underscore')
const glob=require('glob')
const base64Img = require('base64-img')
const os = require('os')
var wsarr=[]
var idx=0
var pics=[]

let basedir
if (os.platform()==='android'){
	basedir="/home/local/web/"
}else{
	basedir=""
}

glob(basedir+'public/images/*.jpeg',(err,files)=>{
     if (!err) {
        pics=files
     }
})

function ws_count(){
	return wsarr.length
}

function add_ws(ws){
		if (_.findIndex(wsarr,(e)=>{
				return ws===e
			} )<0){
			wsarr.push(ws)
		}
}

function del_ws(ws){
	 wsarr=_.without(wsarr,ws)
}

function send_pic(ws){
		 if (os.platform()==='android'){
				 let picstr=android_fetch_pic()
				 if (picstr){
				 		ws.call('update_pic','data:image/jpg;base64,'+picstr,function(err,ret){})
				 }
		 }else{
		 		if (idx>=pics.length){
					return
     		}
		
			 let fn=pics[idx]
			 base64Img.base64(fn, (err, data) => {
                if (err) {
                   console.log("loading gz.jpeg error:", err)
                } else {
									if (typeof ws.call ==='function') {
										ws.call('update_pic',data,function(err,ret){})
									}else{
										console.error(ws)
									}
                }
      })
		}
}

function broadcast_pic(){
		_.each(wsarr,(ws)=>{
			send_pic(ws)	
    })
		idx++
		if (idx>=pics.length){
			idx=0
    }
}

module.exports={
	add_ws,
  del_ws,
  send_pic,
	broadcast_pic,
	ws_count
}

