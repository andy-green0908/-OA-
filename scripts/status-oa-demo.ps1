$ErrorActionPreference = "Continue"

$services = @(
    @{ Port = 3306; Name = "MySQL" },
    @{ Port = 6379; Name = "Redis" },
    @{ Port = 6333; Name = "Qdrant HTTP" },
    @{ Port = 6334; Name = "Qdrant gRPC" },
    @{ Port = 8848; Name = "Nacos" },
    @{ Port = 9200; Name = "Elasticsearch" },
    @{ Port = 11434; Name = "Ollama" },
    @{ Port = 5173; Name = "Frontend" },
    @{ Port = 18080; Name = "Gateway" },
    @{ Port = 18081; Name = "Employee" },
    @{ Port = 18082; Name = "Admin" },
    @{ Port = 18083; Name = "AI" }
)

Write-Host "=== Local ports ==="
foreach ($service in $services) {
    $port = $service.Port
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.Name } else { "unknown" }
        Write-Host ("OK   {0,-5} {1,-16} pid={2,-7} {3}" -f $port, $service.Name, $conn.OwningProcess, $name)
    } else {
        Write-Host ("MISS {0,-5} {1}" -f $port, $service.Name)
    }
}

Write-Host ""
Write-Host "=== AI dependencies ==="
try {
    $qdrant = Invoke-WebRequest -Uri "http://127.0.0.1:6333/healthz" -UseBasicParsing -TimeoutSec 5
    Write-Host "OK   Qdrant health HTTP $($qdrant.StatusCode)"
} catch {
    Write-Host "MISS Qdrant health: $($_.Exception.Message)"
}

try {
    $ollama = Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/version" -TimeoutSec 5
    Write-Host "OK   Ollama version $($ollama.version)"
} catch {
    Write-Host "MISS Ollama health: $($_.Exception.Message)"
}

try {
    $aiHealth = Invoke-RestMethod -Uri "http://127.0.0.1:18083/api/v1/ai/chat/health" -TimeoutSec 10
    $data = $aiHealth.data
    Write-Host "OK   AI health model=$($data.model) embedding=$($data.embeddingModel) vectorReady=$($data.vectorReady) knowledge=$($data.knowledgeCount) vectors=$($data.vectorCount)"
} catch {
    Write-Host "MISS AI health: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "=== Required environment ==="
foreach ($name in @("OA_DB_PASSWORD", "OA_INTERNAL_TOKEN")) {
    $configured = -not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "User"))
    $status = if ($configured) { "OK  " } else { "MISS" }
    Write-Host "$status $name"
}

Write-Host ""
Write-Host "=== FRP client ==="
$frpcProcesses = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "frpc.exe" -and $_.CommandLine -match "frpc.toml" }
if ($frpcProcesses) {
    $frpcProcesses | ForEach-Object { Write-Host "OK   frpc pid=$($_.ProcessId)" }
} else {
    Write-Host "MISS frpc"
}

Write-Host ""
Write-Host "=== Public checks ==="
try {
    $frontend = Invoke-WebRequest -Uri "http://8.148.22.63/" -Method Head -UseBasicParsing -TimeoutSec 10
    Write-Host "OK   public frontend HTTP $($frontend.StatusCode)"
} catch {
    Write-Host "MISS public frontend: $($_.Exception.Message)"
}

try {
    $api = Invoke-WebRequest -Uri "http://8.148.22.63/api/v1/employee/login" -Method Options -Headers @{
        Origin = "http://8.148.22.63"
        "Access-Control-Request-Method" = "POST"
        "Access-Control-Request-Headers" = "content-type"
    } -UseBasicParsing -TimeoutSec 10
    Write-Host "OK   public API preflight HTTP $($api.StatusCode)"
} catch {
    Write-Host "MISS public API: $($_.Exception.Message)"
}

try {
    $ai = Invoke-WebRequest -Uri "http://8.148.22.63/api/v1/ai/chat/health" -UseBasicParsing -TimeoutSec 15
    Write-Host "OK   public AI health HTTP $($ai.StatusCode)"
} catch {
    Write-Host "MISS public AI: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "Public URL: http://8.148.22.63/"
