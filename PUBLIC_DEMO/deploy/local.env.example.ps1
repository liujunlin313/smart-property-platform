# Copy this file to local.env.ps1. That file is ignored by Git.
# Use development-only credentials and never reuse a personal password.

$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/community_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8'
$env:DB_USERNAME = 'smart_property_app'
$env:DB_PASSWORD = '<CHANGE_ME>'

$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_ACCESS_KEY = '<CHANGE_ME>'
$env:MINIO_SECRET_KEY = '<CHANGE_ME>'
$env:MINIO_PROPERTY_BUCKET = 'property-assets'

$env:APP_BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = '<CHANGE_ME_AT_LEAST_10_CHARS>'
$env:APP_BOOTSTRAP_VIEWER_USERNAME = 'viewer'
$env:APP_BOOTSTRAP_VIEWER_PASSWORD = '<CHANGE_ME_AT_LEAST_10_CHARS>'

# Leave blank for a fresh random development key on every backend restart.
$env:APP_JWT_SECRET_BASE64 = ''
$env:APP_REFRESH_COOKIE_SECURE = 'false'
$env:APP_ALLOWED_ORIGINS = 'http://127.0.0.1:5173,http://localhost:5173'
