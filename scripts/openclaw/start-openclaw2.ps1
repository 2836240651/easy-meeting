param(
  [string]$OpenClawDir = 'D:\OpenClaw2',
  [string]$Entry = 'run.bat',
  [string]$Name = 'openclaw2',
  [int]$TailLines = 100,
  [switch]$NoTail
)

$ErrorActionPreference = 'Stop'

function New-EncodedCommand([string]$commandText) {
  $bytes = [System.Text.Encoding]::Unicode.GetBytes($commandText)
  [Convert]::ToBase64String($bytes)
}

if (-not (Test-Path -LiteralPath $OpenClawDir)) {
  throw "OpenClawDir not found: $OpenClawDir"
}

$logRoot = Join-Path $PSScriptRoot 'openclaw-logs'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

$stdout = Join-Path $logRoot "$Name.stdout.log"
$stderr = Join-Path $logRoot "$Name.stderr.log"

("==== {0} starting {1} ====" -f (Get-Date -Format o), $Name) | Add-Content -LiteralPath $stdout -Encoding utf8
("==== {0} starting {1} ====" -f (Get-Date -Format o), $Name) | Add-Content -LiteralPath $stderr -Encoding utf8

$proc = Start-Process `
  -FilePath 'cmd.exe' `
  -ArgumentList @('/c', $Entry) `
  -WorkingDirectory $OpenClawDir `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr `
  -NoNewWindow `
  -PassThru

Write-Host ("Started {0} (PID {1})" -f $Name, $proc.Id)
Write-Host ("Stdout: {0}" -f $stdout)
Write-Host ("Stderr: {0}" -f $stderr)

if ($NoTail) {
  Write-Host "NoTail enabled; exiting without log tail."
  exit 0
}

$tailOutCmd = @'
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -LiteralPath '{0}' -Tail {1} -Wait | ForEach-Object {{ "[{2}][out] $($_)" }}
'@ -f ($stdout.Replace("'", "''")), $TailLines, $Name

$tailErrCmd = @'
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -LiteralPath '{0}' -Tail {1} -Wait | ForEach-Object {{ "[{2}][err] $($_)" }}
'@ -f ($stderr.Replace("'", "''")), $TailLines, $Name

$tailOut = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy', 'Bypass',
  '-EncodedCommand', (New-EncodedCommand $tailOutCmd)
) -NoNewWindow -PassThru

$tailErr = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy', 'Bypass',
  '-EncodedCommand', (New-EncodedCommand $tailErrCmd)
) -NoNewWindow -PassThru

$script:cancelRequested = $false
$cancelHandler = [ConsoleCancelEventHandler]{
  param($sender, $e)
  $e.Cancel = $true
  $script:cancelRequested = $true
}
[Console]::add_CancelKeyPress($cancelHandler)

try {
  while ($true) {
    $proc.Refresh()
    if ($proc.HasExited) { break }
    if ($script:cancelRequested) { break }
    Start-Sleep -Milliseconds 250
  }
} finally {
  [Console]::remove_CancelKeyPress($cancelHandler)
  Stop-Process -Id $tailOut.Id -Force -ErrorAction SilentlyContinue
  Stop-Process -Id $tailErr.Id -Force -ErrorAction SilentlyContinue
}

if ($script:cancelRequested -and -not $proc.HasExited) {
  Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
