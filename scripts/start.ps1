param([switch]$Check, [switch]$NoBrowser)
. "$PSScriptRoot/common.ps1"
$lock = $null
$started = [Collections.Generic.List[string]]::new()
try {
    $lock = Lock-Launcher
    Import-Settings
    foreach ($name in @('DB_URL', 'DB_USER', 'JWT_SECRET', 'ADMIN_PASSWORD', 'MINIO_ENDPOINT', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY')) {
        if (!(Get-Setting $name)) { throw "Missing configuration: $name" }
    }
    if ([Text.Encoding]::UTF8.GetByteCount((Get-Setting 'JWT_SECRET')) -lt 32) { throw 'JWT_SECRET must contain at least 32 UTF-8 bytes.' }
    $adminLength = (Get-Setting 'ADMIN_PASSWORD').Length
    if ($adminLength -lt 8 -or $adminLength -gt 64) { throw 'ADMIN_PASSWORD must contain 8 to 64 characters.' }
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    $prefix = Get-Setting 'UPLOAD_URL_PREFIX' '/uploads/'
    if ($prefix -notmatch '^/(?:[A-Za-z0-9_-]+/)+$') { throw 'UPLOAD_URL_PREFIX must be an absolute URL path ending with /, such as /uploads/.' }
    $appPort = Get-Port 'APP_PORT' 8081
    $webPort = Get-Port 'WEB_PORT' 5173
    if ($appPort -eq $webPort) { throw 'APP_PORT and WEB_PORT must differ.' }
    $dbUrl = Get-Setting 'DB_URL'
    if (!$dbUrl.StartsWith('jdbc:mysql://')) { throw 'DB_URL must start with jdbc:mysql://.' }
    $database = [Uri]$dbUrl.Substring(5)
    $dbPort = if ($database.Port -gt 0) { $database.Port } else { 3306 }
    if (!(Test-Tcp $database.Host $dbPort)) { throw 'MySQL is unreachable. Start your MySQL service first.' }
    $minio = [Uri](Get-Setting 'MINIO_ENDPOINT')
    if ($minio.Scheme -notin @('http', 'https')) { throw 'MINIO_ENDPOINT must be an HTTP(S) URL.' }
    if (!(Test-Http ($minio.AbsoluteUri.TrimEnd('/') + '/minio/health/live'))) { throw 'MinIO is unreachable. Start your existing MinIO service first.' }
    $backendUrl = "http://127.0.0.1:$appPort"
    $frontendUrl = "http://127.0.0.1:$webPort"
    $running = @{}
    foreach ($item in @(@{Name='backend';Port=$appPort}, @{Name='frontend';Port=$webPort})) {
        $state = Join-Path $RuntimeDir ($item.Name + '.json')
        $process = $null
        if (Test-Path -LiteralPath $state) { $process = Get-OwnedProcess (Get-Content -LiteralPath $state -Raw | ConvertFrom-Json) }
        if ($process) { $running[$item.Name] = $process }
        elseif (Test-Tcp '127.0.0.1' $item.Port) { throw "Port $($item.Port) is in use outside this launcher. Stop that service or change the port in .env." }
    }
    Write-Host 'Configuration, tools, MySQL, MinIO and ports checked. Database credentials and schema are verified when the backend starts.'
    if ($Check) { exit 0 }
    $backend = Join-Path $ProjectRoot 'backend'
    $frontend = Join-Path $ProjectRoot 'frontend'
    if (!$running.ContainsKey('backend')) {
        Write-Host 'Building backend...'
        Push-Location $backend
        try { & $maven '-q' '-DskipTests' 'package'; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' } } finally { Pop-Location }
        $jar = Join-Path $backend 'app/target/vibe-backend.jar'
        $running['backend'] = Start-Owned 'backend' $java @('-jar', ('"' + $jar + '"')) $ProjectRoot
        $started.Add('backend')
    }
    Wait-Http "$backendUrl/music/public/styles" $running['backend'] 'backend'
    if (!$running.ContainsKey('frontend')) {
        $lockHash = (Get-FileHash -LiteralPath (Join-Path $frontend 'package-lock.json') -Algorithm SHA256).Hash
        $stamp = Join-Path $RuntimeDir 'frontend-lock.txt'
        $installedHash = if (Test-Path -LiteralPath $stamp) { (Get-Content -LiteralPath $stamp -Raw).Trim() } else { '' }
        if ($installedHash -ne $lockHash -or !(Test-Path -LiteralPath (Join-Path $frontend 'node_modules/vite/bin/vite.js'))) {
            Write-Host 'Installing frontend dependencies...'
            Push-Location $frontend
            try { & $npm 'ci'; if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }; Set-Content -LiteralPath $stamp -Value $lockHash -Encoding ASCII } finally { Pop-Location }
        }
        $running['frontend'] = Start-Owned 'frontend' $node @('node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', "$webPort", '--strictPort') $frontend
        $started.Add('frontend')
    }
    Wait-Http $frontendUrl $running['frontend'] 'frontend'
    Write-Host "Ready: $frontendUrl"
    Write-Host 'Use stop.cmd to stop these application processes. MySQL and MinIO stay running.'
    if (!$NoBrowser) { Start-Process $frontendUrl }
} catch {
    for ($i = $started.Count - 1; $i -ge 0; $i--) { Stop-Owned $started[$i] }
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally { if ($lock) { $lock.Dispose() } }
