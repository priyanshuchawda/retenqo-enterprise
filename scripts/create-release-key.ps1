$ErrorActionPreference = 'Stop'

$androidDir = Join-Path $env:USERPROFILE '.android'
$keyFile = Join-Path $androidDir 'retenqo-release.p12'
$passwordFile = Join-Path $androidDir 'retenqo-release-password.dpapi'
$keyTool = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\keytool.exe'
} else {
    (Get-Command keytool.exe -ErrorAction Stop).Source
}

if (Test-Path -LiteralPath $keyFile -PathType Leaf) {
    throw "Refusing to replace an existing release key: $keyFile"
}
if (Test-Path -LiteralPath $passwordFile -PathType Leaf) {
    throw "Refusing to replace existing protected signing credentials: $passwordFile"
}

[System.IO.Directory]::CreateDirectory($androidDir) | Out-Null
$temporaryKey = "$keyFile.$PID.tmp"
$temporaryPasswordFile = "$passwordFile.$PID.tmp"
$randomBytes = [byte[]]::new(36)
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($randomBytes)
$password = [Convert]::ToBase64String($randomBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$env:RETENQO_GENERATED_RELEASE_PASSWORD = $password

try {
    & $keyTool -genkeypair `
        -alias retenqo-release `
        -keyalg RSA `
        -keysize 3072 `
        -validity 10000 `
        -dname 'CN=Retenqo Android Release' `
        -keystore $temporaryKey `
        -storetype PKCS12 `
        -storepass:env RETENQO_GENERATED_RELEASE_PASSWORD `
        -keypass:env RETENQO_GENERATED_RELEASE_PASSWORD `
        -noprompt | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

    $securePassword = ConvertTo-SecureString $password -AsPlainText -Force
    $protectedPassword = ConvertFrom-SecureString $securePassword
    [System.IO.File]::WriteAllText($temporaryPasswordFile, $protectedPassword)

    Move-Item -LiteralPath $temporaryPasswordFile -Destination $passwordFile
    Move-Item -LiteralPath $temporaryKey -Destination $keyFile

    $fingerprintOutput = & $keyTool -list -v `
        -alias retenqo-release `
        -keystore $keyFile `
        -storetype PKCS12 `
        -storepass:env RETENQO_GENERATED_RELEASE_PASSWORD 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Unable to verify the generated release key" }
    $fingerprint = $fingerprintOutput | Select-String 'SHA256:' | Select-Object -First 1
    Write-Output "RELEASE_KEY_CREATED keystore=$keyFile password_protection=Windows_DPAPI"
    if ($fingerprint) { Write-Output $fingerprint.ToString().Trim() }
    Write-Output 'Keep both files together for future signed updates; neither is stored in Git.'
} finally {
    Remove-Item Env:\RETENQO_GENERATED_RELEASE_PASSWORD -ErrorAction SilentlyContinue
    $rng.Dispose()
    [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    $password = $null
}
