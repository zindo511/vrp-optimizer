const http = require('http');

const data = JSON.stringify({ email: "admin@vrp.vn", password: "123456" });

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

const req = http.request(options, res => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    const token = JSON.parse(body).data.token;
    
    const req2 = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/routes?date=2026-05-03',
      method: 'GET',
      headers: { 'Authorization': 'Bearer ' + token }
    }, res2 => {
      let body2 = '';
      res2.on('data', d => body2 += d);
      res2.on('end', () => {
        console.log(JSON.stringify(JSON.parse(body2), null, 2));
      });
    });
    req2.end();
  });
});

req.write(data);
req.end();
