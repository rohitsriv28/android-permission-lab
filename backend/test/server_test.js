import http from 'http';
import assert from 'assert';
import app from '../src/app.js';

const testServer = () => {
  return new Promise((resolve, reject) => {
    const server = app.listen(0, '127.0.0.1', () => {
      const port = server.address().port;
      console.log(`Test server running on port ${port}`);

      // Test GET /api/health
      http.get(`http://127.0.0.1:${port}/api/health`, (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            console.log('Health Endpoint Response:', json);
            assert.strictEqual(json.success, true);
            assert.strictEqual(typeof json.data.status, 'string');
            server.close(() => {
              console.log('✅ Health API Test Passed Successfully!');
              resolve();
            });
          } catch (err) {
            server.close();
            reject(err);
          }
        });
      }).on('error', (err) => {
        server.close();
        reject(err);
      });
    });
  });
};

testServer().catch((err) => {
  console.error('❌ Test Failed:', err);
  process.exit(1);
});
