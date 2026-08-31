$ErrorActionPreference = 'Stop'
if (-not $env:TRESTLE_WINDOWS_CERTIFICATE_BASE64) { throw 'The Windows signing certificate is not configured.' }
$certificatePath = Join-Path $env:RUNNER_TEMP 'trestle-signing.pfx'
$certificate = $null
try {
    [IO.File]::WriteAllBytes($certificatePath, [Convert]::FromBase64String($env:TRESTLE_WINDOWS_CERTIFICATE_BASE64))
    $password = ConvertTo-SecureString $env:TRESTLE_WINDOWS_CERTIFICATE_PASSWORD -AsPlainText -Force
    $certificate = Import-PfxCertificate -FilePath $certificatePath -CertStoreLocation Cert:\CurrentUser\My -Password $password
    $signTool = Get-ChildItem 'C:\Program Files (x86)\Windows Kits\10\bin\*\x64\signtool.exe' | Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $signTool) { throw 'Windows SDK signtool.exe was not found.' }
    $packages = Get-ChildItem 'desktopApp/build/compose/binaries' -Recurse -File | Where-Object { $_.Extension -in '.msi', '.exe' }
    if ($packages.Count -eq 0) { throw 'No Windows packages were found.' }
    foreach ($package in $packages) {
        & $signTool.FullName sign /sha1 $certificate.Thumbprint /fd SHA256 /tr 'http://timestamp.digicert.com' /td SHA256 /d 'Trestle' /du 'https://github.com/6b6t/trestle' $package.FullName
        if ($LASTEXITCODE -ne 0) { throw "Signing failed: $($package.Name)" }
        & $signTool.FullName verify /pa /all $package.FullName
        if ($LASTEXITCODE -ne 0) { throw "Signature verification failed: $($package.Name)" }
    }
} finally {
    Remove-Item $certificatePath -ErrorAction SilentlyContinue
    if ($certificate) { Remove-Item "Cert:\CurrentUser\My\$($certificate.Thumbprint)" -ErrorAction SilentlyContinue }
}
