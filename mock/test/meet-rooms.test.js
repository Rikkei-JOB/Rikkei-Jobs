const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const net = require('node:net');
let child, base;
before(async () => {
  const server = net.createServer();
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  const port=server.address().port;
  await new Promise(resolve=>server.close(resolve));
  base=`http://127.0.0.1:${port}`;
  child=spawn(process.execPath,['server.js'],{cwd:require('node:path').resolve(__dirname,'..'),windowsHide:true,env:{...process.env,PORT:String(port),SERVICE_KEY:'ut06-key'},stdio:['ignore','pipe','pipe']});
  await new Promise((resolve,reject)=>{child.stdout.on('data',resolve);child.on('error',reject);child.on('exit',code=>reject(new Error(`mock exit ${code}`)));});
});
after(()=>child?.kill());
const payload={title:'Interview',startAt:'2030-01-01T09:00:00Z',endAt:'2030-01-01T10:00:00Z'};
function room(key,body=payload) { return fetch(`${base}/meet/rooms`,{method:'POST',headers:{'Content-Type':'application/json','X-Service-Key':'ut06-key',...(key?{'Idempotency-Key':key}:{})},body:JSON.stringify(body)}); }
test('same key reuses room; changed payload conflicts; reset clears key',async()=>{
  const first=await (await room('slot-1')).json();
  const second=await (await room('slot-1')).json();
  assert.equal(second.roomUrl,first.roomUrl);
  assert.equal((await room('slot-1',{...payload,title:'Changed'})).status,409);
  await fetch(`${base}/mock/reset`,{method:'POST'});
  assert.notEqual((await (await room('slot-1')).json()).roomId,first.roomId);
});
test('legacy calls without a key still create distinct rooms',async()=>{
  assert.notEqual((await (await room()).json()).roomId,(await (await room()).json()).roomId);
});
