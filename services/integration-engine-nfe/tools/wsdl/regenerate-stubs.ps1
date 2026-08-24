[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $CertificatePath,

    [Parameter(Mandatory = $true)]
    [SecureString] $CertificatePassword,

    [string] $Axis2Home = $env:AXIS2_HOME,

    [switch] $DownloadOnly
)

$ErrorActionPreference = 'Stop'
$toolDirectory = $PSScriptRoot
$projectDirectory = (Resolve-Path (Join-Path $toolDirectory '..\..')).Path
$manifestPath = Join-Path $toolDirectory 'contracts.csv'
$wsdlDirectory = Join-Path $projectDirectory 'target\wsdl-official'
$generatedDirectory = Join-Path $projectDirectory 'target\regenerated-axis2'

if (-not (Test-Path -LiteralPath $CertificatePath -PathType Leaf)) {
    throw "Certificado nao encontrado: $CertificatePath"
}

if (-not $DownloadOnly) {
    if ([string]::IsNullOrWhiteSpace($Axis2Home)) {
        throw 'Informe -Axis2Home ou defina AXIS2_HOME com o Apache Axis2 2.0.1.'
    }
    $wsdl2Java = Join-Path $Axis2Home 'bin\WSDL2Java.bat'
    if (-not (Test-Path -LiteralPath $wsdl2Java -PathType Leaf)) {
        throw "WSDL2Java.bat nao encontrado em: $wsdl2Java"
    }
}

New-Item -ItemType Directory -Force -Path $wsdlDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $generatedDirectory | Out-Null

$certificate = [System.Security.Cryptography.X509Certificates.X509Certificate2]::new(
    (Resolve-Path -LiteralPath $CertificatePath).Path,
    $CertificatePassword,
    [System.Security.Cryptography.X509Certificates.X509KeyStorageFlags]::EphemeralKeySet
)

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.ClientCertificates.Add($certificate)
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(60)

try {
    foreach ($contract in (Import-Csv -LiteralPath $manifestPath)) {
        $wsdlPath = Join-Path $wsdlDirectory ($contract.Name + '.wsdl')
        Write-Host "Baixando $($contract.Name)"
        $content = $client.GetStringAsync($contract.Url).GetAwaiter().GetResult()
        if ($content -notmatch '<(?:\w+:)?definitions\b') {
            throw "A resposta de $($contract.Url) nao e um WSDL valido."
        }
        [System.IO.File]::WriteAllText($wsdlPath, $content, [System.Text.UTF8Encoding]::new($false))

        if (-not $DownloadOnly) {
            $contractOutput = Join-Path $generatedDirectory $contract.Name
            Write-Host "Gerando $($contract.Package)"
            & $wsdl2Java '-uri' $wsdlPath '-p' $contract.Package '-d' 'adb' '-s' '-o' $contractOutput
            if ($LASTEXITCODE -ne 0) {
                throw "Axis2 falhou ao gerar $($contract.Name). Codigo: $LASTEXITCODE"
            }
        }
    }
}
finally {
    $client.Dispose()
    $handler.Dispose()
    $certificate.Dispose()
}

Write-Host "WSDLs: $wsdlDirectory"
if (-not $DownloadOnly) {
    Write-Host "Stubs para revisao: $generatedDirectory"
}
