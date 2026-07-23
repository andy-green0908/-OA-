param(
    [switch]$IncludeInfra
)

$ErrorActionPreference = "Continue"

$DemoPorts = @(5173, 18080, 18081, 18082, 18083)
$InfraPorts = @(6379, 8848, 9200)

function Stop-ListeningProcess {
    param([int]$Port)
    $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($conn in $connections) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "Stopping port $Port pid=$($proc.ProcessId)"
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "Stopping OA demo app processes..."
foreach ($port in $DemoPorts) {
    Stop-ListeningProcess $port
}

$frpcProcesses = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "frpc.exe" -and $_.CommandLine -match "frpc.toml" }
foreach ($proc in $frpcProcesses) {
    Write-Host "Stopping frpc pid=$($proc.ProcessId)"
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
}

if ($IncludeInfra) {
    Write-Host "Stopping infrastructure processes: Redis, Nacos, Elasticsearch, Qdrant, Ollama..."
    foreach ($port in $InfraPorts) {
        Stop-ListeningProcess $port
    }
    if (docker ps --filter "name=^nexus-qdrant$" --format "{{.Names}}") {
        docker stop nexus-qdrant | Out-Null
        Write-Host "Stopped Qdrant container nexus-qdrant"
    }
    Stop-ListeningProcess 11434
} else {
    Write-Host "Infrastructure kept running. Use -IncludeInfra to also stop Redis/Nacos/Elasticsearch/Qdrant/Ollama."
}

Write-Host "Stop command finished."
