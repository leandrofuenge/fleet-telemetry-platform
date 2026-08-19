param(
    [string]$BaseUrl = "http://localhost:9060",
    [Parameter(Mandatory = $true)][string]$ApiKey,
    [Parameter(Mandatory = $true)][string]$Placa,
    [Parameter(Mandatory = $true)][string]$Renavam
)

$ErrorActionPreference = "Stop"
$correlationId = "homologation-" + [guid]::NewGuid().ToString()
$headers = @{
    "X-Integration-API-Key" = $ApiKey
    "X-Correlation-ID" = $correlationId
}
$payload = @{ placa = $Placa; renavam = $Renavam } | ConvertTo-Json

Write-Host "Checking service health..."
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
if ($health.status -ne "UP") {
    throw "Service health is $($health.status). Check secrets and outbound connectivity."
}

Write-Host "Executing SERPRO query with correlation ID $correlationId..."
$response = Invoke-RestMethod -Method Post `
    -Uri "$BaseUrl/api/integracoes/senatran/serpro/veiculos/consulta" `
    -Headers $headers -ContentType "application/json" -Body $payload

$response | ConvertTo-Json -Depth 8
