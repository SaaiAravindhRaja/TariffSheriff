const https = require('https')
const fs = require('fs')
const url = 'https://github.com/user-attachments/assets/f63f8f5b-6540-4f21-9f6f-508ea4254337'
const out = 'apps/frontend/public/logo.png'

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest)
    https.get(url, (res) => {
      if (res.statusCode >= 400) return reject(new Error('Failed to download, status ' + res.statusCode))
      res.pipe(file)
      file.on('finish', () => file.close(resolve))
    }).on('error', (err) => {
      fs.unlink(dest, () => {})
      reject(err)
    })
  })
}

download(url, out)
  .then(() => console.log('Logo downloaded to', out))
  .catch((e) => {
    console.error('Failed to download logo:', e.message)
    process.exit(1)
  })
