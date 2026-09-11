$portNumber = 0
$portValid = [int]::TryParse($env:MAIL_PORT, [ref]$portNumber)

Write-Output ("MAIL_HOST_SET=" + (-not [string]::IsNullOrWhiteSpace($env:MAIL_HOST)))
Write-Output ("MAIL_PORT_VALID=" + $portValid)
Write-Output ("MAIL_USERNAME_SET=" + (-not [string]::IsNullOrWhiteSpace($env:MAIL_USERNAME)))
Write-Output ("MAIL_PASSWORD_SET=" + (-not [string]::IsNullOrWhiteSpace($env:MAIL_PASSWORD)))
Write-Output ("MAIL_FROM_SET=" + (-not [string]::IsNullOrWhiteSpace($env:MAIL_FROM)))
Write-Output ("MAIL_FROM_MATCHES_USERNAME=" + ($env:MAIL_FROM -eq $env:MAIL_USERNAME))
Write-Output ("MAIL_HOST_IS_LOCAL=" + (@("localhost", "127.0.0.1", "::1") -contains $env:MAIL_HOST.ToLowerInvariant()))
Write-Output ("MAIL_PORT_IS_LOCAL_DEV=" + ($portNumber -eq 1025))

$mailHost = $env:MAIL_HOST.ToLowerInvariant()
$provider = if ($mailHost -match "gmail") {
    "GMAIL"
} elseif ($mailHost -match "office365|outlook") {
    "MICROSOFT"
} elseif ($mailHost -match "brevo|sendinblue") {
    "BREVO"
} elseif ($mailHost -match "mailtrap") {
    "MAILTRAP"
} elseif ($mailHost -match "sendgrid") {
    "SENDGRID"
} elseif ($mailHost -match "resend") {
    "RESEND"
} else {
    "OTHER"
}
Write-Output ("SMTP_PROVIDER=" + $provider)

if ($portValid) {
    if ($portNumber -eq 465) {
        Write-Output "TLS_MODE_REQUIRED=IMPLICIT_SSL"
    } elseif ($portNumber -eq 587) {
        Write-Output "TLS_MODE_REQUIRED=STARTTLS"
    } else {
        Write-Output "TLS_MODE_REQUIRED=PROVIDER_SPECIFIC"
    }

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $pending = $client.BeginConnect($env:MAIL_HOST, $portNumber, $null, $null)
        $connected = $pending.AsyncWaitHandle.WaitOne(5000) -and $client.Connected
        Write-Output ("SMTP_TCP_REACHABLE=" + $connected)
    } catch {
        Write-Output "SMTP_TCP_REACHABLE=False"
    } finally {
        $client.Dispose()
    }
}
