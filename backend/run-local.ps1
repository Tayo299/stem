$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function Read-SecretValue([string]$Prompt) {
    $secureValue = Read-Host -Prompt $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/hssc'
$env:DATABASE_USERNAME = 'hssc'
$env:DATABASE_PASSWORD = Read-SecretValue 'Enter the password you set for the PostgreSQL hssc role'

$randomBytes = New-Object byte[] 32
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($randomBytes)
}
finally {
    $generator.Dispose()
}
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
$env:JWT_ISSUER = 'hssc-api'
$env:JWT_EXPIRATION_MINUTES = '60'
$env:UPLOAD_DIRECTORY = Join-Path $PSScriptRoot 'var\uploads'
$env:HSSC_DISTRICT_ZONES = 'I=District I|Agege,Alimosho,Ifako-Ijaiye;II=District II|Ikorodu,Kosofe,Somolu;III=District III|Epe,Eti-Osa,Ibeju-Lekki,Lagos Island;IV=District IV|Apapa,Mainland,Surulere;V=District V|Ajeromi-Ifelodun,Amuwo-Odofin,Badagry,Ojo;VI=District VI|Ikeja,Mushin,Oshodi-Isolo'

$adminEmail = Read-Host 'Enter an initial admin email (leave blank to skip admin creation)'
if (-not [string]::IsNullOrWhiteSpace($adminEmail)) {
    $adminPassword = Read-SecretValue 'Enter an initial admin password (at least 16 characters)'
    if ($adminPassword.Length -lt 16) {
        throw 'The initial admin password must contain at least 16 characters.'
    }
    $env:HSSC_ADMIN_EMAIL = $adminEmail.Trim()
    $env:HSSC_ADMIN_PASSWORD = $adminPassword
}
else {
    Remove-Item Env:HSSC_ADMIN_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:HSSC_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host 'Starting the HSSC API at http://localhost:8080'
Write-Host 'Health check: http://localhost:8080/actuator/health'
Write-Host 'Press Ctrl+C to stop the server.'
Write-Host ''

& mvn spring-boot:run
exit $LASTEXITCODE
