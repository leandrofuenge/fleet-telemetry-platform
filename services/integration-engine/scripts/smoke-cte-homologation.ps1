param(
    [string]$BaseUrl = "http://localhost:9060",
    [ValidatePattern("^[A-Z]{2}$")][string]$Uf = "MT",
    [int]$TimeoutSeconds = 30
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

function Invoke-SmokeGet([string]$Uri) {
    return Invoke-RestMethod -Method Get -Uri $Uri -TimeoutSec $TimeoutSeconds `
        -Headers @{ "X-Correlation-ID" = "cte-smoke-$([guid]::NewGuid())" }
}

Write-Host "[1/3] Verificando health da aplicação..."
$health = Invoke-SmokeGet "$base/actuator/health"
if ($health.status -ne "UP") {
    throw "Health retornou '$($health.status)' em vez de UP."
}

Write-Host "[2/3] Consultando apenas o status CT-e em homologação para $Uf..."
$status = Invoke-SmokeGet "$base/api/integracoes/sefaz/cte/status?uf=$Uf&ambiente=homologacao"
if ($status.ambiente -notmatch "(?i)HOMOLOGACAO") {
    throw "Resposta retornou ambiente inesperado: '$($status.ambiente)'."
}
if ([string]$status.codigo -notin @("107", "113")) {
    throw "SEFAZ indisponível ou resposta inesperada: cStat=$($status.codigo), motivo=$($status.mensagem)"
}
if (-not $status.disponivel) {
    throw "Resposta possui código operacional, mas disponivel=false."
}

Write-Host "[3/3] Confirmando evidência de chamada real..."
if ($status.simulado) {
    throw "O endpoint respondeu em modo simulado; o smoke test de homologação exige chamada mTLS real."
}
if ([string]::IsNullOrWhiteSpace([string]$status.xmlRetornoSoap)) {
    throw "Resposta não contém XML SOAP de retorno da SEFAZ."
}

[pscustomobject]@{
    resultado = "OK"
    uf = $Uf
    ambiente = $status.ambiente
    cStat = $status.codigo
    motivo = $status.mensagem
    simulado = $status.simulado
    tempoRespostaMs = $status.tempoRespostaMs
} | ConvertTo-Json
