$ErrorActionPreference = "Stop"

$BackendDir = "D:\nexus-office\Nacos-SpringBoot-oa3\backend"
$FrontendDir = "D:\nexus-office\frontend"
$FrpDir = "C:\frp"
$NacosDir = "D:\tool\nacos"
$EsDir = "D:\tool\elasticsearch-7.13.0"
$RedisExe = "D:\tool\redis-2.8.9\redis-server.exe"
$DockerDesktopExe = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
$QdrantContainer = "nexus-qdrant"
$OllamaExe = (Get-Command "ollama.exe" -ErrorAction SilentlyContinue).Source

$JavaExe = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    (Get-Command "java.exe" -ErrorAction SilentlyContinue).Source
}

$LogDir = Join-Path $BackendDir "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Test-Port {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Test-TcpEndpoint {
    param(
        [string]$HostName,
        [int]$Port
    )
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne(2000, $false)) {
            return $false
        }
        $client.EndConnect($iar)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 45
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) {
            Write-Host "OK: port $Port is listening"
            return
        }
        Start-Sleep -Seconds 2
    }
    Write-Warning "Port $Port did not start within $TimeoutSeconds seconds"
}

function Import-RequiredUserEnvironment {
    param([string[]]$Names)
    foreach ($name in $Names) {
        $value = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrWhiteSpace($value)) {
            $value = [Environment]::GetEnvironmentVariable($name, "User")
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
        if ([string]::IsNullOrWhiteSpace($value)) {
            throw "Required environment variable is missing: $name"
        }
        Write-Host "OK: environment variable $name is configured"
    }
}

function Test-DockerEngine {
    docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Start-LoggedProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory,
        [string]$OutLog,
        [string]$ErrLog
    )
    Write-Host "Starting $Name..."
    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $OutLog `
        -RedirectStandardError $ErrLog `
        -WindowStyle Hidden `
        -PassThru
    Write-Host "$Name pid=$($process.Id)"
}

function Stop-ListeningProcess {
    param([int]$Port)
    $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($conn in $connections) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "Stopping stale listener on port $Port pid=$($proc.ProcessId)"
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "=== OA demo startup ==="

Import-RequiredUserEnvironment @("OA_DB_PASSWORD", "OA_INTERNAL_TOKEN")

if (-not (Test-Port 3306)) {
    $mysqlService = Get-Service | Where-Object { $_.Name -like "MySQL*" } | Select-Object -First 1
    if ($mysqlService) {
        Write-Host "Starting MySQL service $($mysqlService.Name)..."
        Start-Service $mysqlService.Name
        Wait-Port 3306 30
    } else {
        Write-Warning "MySQL is not listening on 3306, and no MySQL service was found."
    }
} else {
    Write-Host "OK: MySQL port 3306 is already listening"
}

if (-not (Test-Port 6379)) {
    if (Test-Path $RedisExe) {
        Start-LoggedProcess "Redis" $RedisExe @() (Split-Path $RedisExe) (Join-Path $LogDir "redis.out.log") (Join-Path $LogDir "redis.err.log")
        Wait-Port 6379 20
    } else {
        Write-Warning "Redis executable not found: $RedisExe"
    }
} else {
    Write-Host "OK: Redis port 6379 is already listening"
}

if (-not (Test-Port 8848)) {
    $nacosStartup = Join-Path $NacosDir "bin\startup.cmd"
    if (Test-Path $nacosStartup) {
        Write-Host "Starting Nacos..."
        Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $nacosStartup, "-m", "standalone") -WorkingDirectory (Join-Path $NacosDir "bin") -WindowStyle Hidden
        Wait-Port 8848 80
    } else {
        Write-Warning "Nacos startup script not found: $nacosStartup"
    }
} else {
    Write-Host "OK: Nacos port 8848 is already listening"
}

if (-not (Test-Port 9200)) {
    $esStartup = Join-Path $EsDir "bin\elasticsearch.bat"
    if (Test-Path $esStartup) {
        Write-Host "Starting Elasticsearch..."
        Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $esStartup) -WorkingDirectory $EsDir -WindowStyle Hidden
        Wait-Port 9200 120
    } else {
        Write-Warning "Elasticsearch startup script not found: $esStartup"
    }
} else {
    Write-Host "OK: Elasticsearch port 9200 is already listening"
}

if (-not (Test-DockerEngine)) {
    if (-not (Test-Path $DockerDesktopExe)) {
        throw "Docker Desktop not found: $DockerDesktopExe"
    }
    Write-Host "Starting Docker Desktop..."
    Start-Process -FilePath $DockerDesktopExe -WindowStyle Hidden
    $dockerDeadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $dockerDeadline -and -not (Test-DockerEngine)) {
        Start-Sleep -Seconds 2
    }
    if (-not (Test-DockerEngine)) {
        throw "Docker Engine did not start within 90 seconds"
    }
    Write-Host "OK: Docker Engine is ready"
} else {
    Write-Host "OK: Docker Engine is already ready"
}

$qdrantExists = docker ps -a --filter "name=^$QdrantContainer$" --format "{{.Names}}"
if ($qdrantExists -ne $QdrantContainer) {
    throw "Qdrant container does not exist: $QdrantContainer"
}
if (-not (Test-Port 6333)) {
    Write-Host "Starting Qdrant container $QdrantContainer..."
    docker start $QdrantContainer | Out-Null
    Wait-Port 6333 45
} else {
    Write-Host "OK: Qdrant port 6333 is already listening"
}

if (-not (Test-Port 11434)) {
    if (-not $OllamaExe) {
        throw "ollama.exe was not found in PATH"
    }
    Start-LoggedProcess "Ollama" $OllamaExe @("serve") (Split-Path $OllamaExe) (Join-Path $LogDir "ollama.out.log") (Join-Path $LogDir "ollama.err.log")
    Wait-Port 11434 60
} else {
    Write-Host "OK: Ollama port 11434 is already listening"
}

foreach ($model in @("qwen2.5:7b", "bge-m3")) {
    $installed = ollama list | Select-String -SimpleMatch $model
    if ($installed) {
        Write-Host "OK: Ollama model $model is installed"
    } else {
        Write-Warning "Ollama model is missing: $model"
    }
}

if (-not $JavaExe -or -not (Test-Path $JavaExe)) {
    throw "Java executable was not found. Configure JAVA_HOME or PATH with Java 21."
}

if (-not (Test-Port 18081)) {
    Start-LoggedProcess "OA employee service" $JavaExe @("-Xms128m", "-Xmx256m", "-jar", "OA-2\target\oa-emp-service-1.0.0.jar") $BackendDir (Join-Path $LogDir "emp.out.log") (Join-Path $LogDir "emp.err.log")
    Wait-Port 18081 70
} else {
    Write-Host "OK: employee service port 18081 is already listening"
}

if (-not (Test-Port 18082)) {
    Start-LoggedProcess "OA admin service" $JavaExe @("-Xms128m", "-Xmx256m", "-jar", "OA-7\target\oa-admin-service-1.0.0.jar") $BackendDir (Join-Path $LogDir "admin.out.log") (Join-Path $LogDir "admin.err.log")
    Wait-Port 18082 70
} else {
    Write-Host "OK: admin service port 18082 is already listening"
}

if (-not (Test-Port 18083)) {
    Start-LoggedProcess "OA AI service" $JavaExe @("-Xms256m", "-Xmx768m", "-jar", "oa-ai-service\target\oa-ai-service-1.0.0.jar") $BackendDir (Join-Path $LogDir "ai.out.log") (Join-Path $LogDir "ai.err.log")
    Wait-Port 18083 150
} else {
    Write-Host "OK: AI service port 18083 is already listening"
}

if (-not (Test-Port 18080)) {
    Start-LoggedProcess "OA gateway" $JavaExe @("-Xms128m", "-Xmx256m", "-jar", "gateway\target\gateway-1.0.0.jar") $BackendDir (Join-Path $LogDir "gateway.out.log") (Join-Path $LogDir "gateway.err.log")
    Wait-Port 18080 70
} else {
    Write-Host "OK: gateway port 18080 is already listening"
}

if ((Test-Port 5173) -and -not (Test-TcpEndpoint "127.0.0.1" 5173)) {
    Write-Warning "Frontend port 5173 is listening, but not on 127.0.0.1. Restarting frontend for frpc."
    Stop-ListeningProcess 5173
    Start-Sleep -Seconds 2
}

if (-not (Test-TcpEndpoint "127.0.0.1" 5173)) {
    $npm = "npm.cmd"
    if (Test-Path (Join-Path $FrontendDir "dist\index.html")) {
        Start-LoggedProcess "OA frontend preview" $npm @("run", "preview", "--", "--host", "127.0.0.1", "--port", "5173") $FrontendDir (Join-Path $LogDir "frontend.out.log") (Join-Path $LogDir "frontend.err.log")
    } else {
        Write-Warning "Frontend dist not found. Starting Vite dev server instead of preview."
        Start-LoggedProcess "OA frontend dev" $npm @("run", "dev", "--", "--host", "127.0.0.1", "--port", "5173") $FrontendDir (Join-Path $LogDir "frontend.out.log") (Join-Path $LogDir "frontend.err.log")
    }
    Wait-Port 5173 45
} else {
    Write-Host "OK: frontend port 5173 is already listening on 127.0.0.1"
}

if (-not (Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "frpc.exe" -and $_.CommandLine -match "frpc.toml" })) {
    $frpc = Join-Path $FrpDir "frpc.exe"
    $frpcConfig = Join-Path $FrpDir "frpc.toml"
    if ((Test-Path $frpc) -and (Test-Path $frpcConfig)) {
        Start-LoggedProcess "FRP client" $frpc @("-c", ".\frpc.toml") $FrpDir (Join-Path $LogDir "frpc.out.log") (Join-Path $LogDir "frpc.err.log")
        Start-Sleep -Seconds 5
    } else {
        Write-Warning "frpc.exe or frpc.toml not found in $FrpDir"
    }
} else {
    Write-Host "OK: frpc is already running"
}

Write-Host ""
Write-Host "Startup command finished."
Write-Host "Public URL: http://8.148.22.63/"
Write-Host "Run status script if anything looks wrong: D:\nexus-office\scripts\status-oa-demo.ps1"
