param(
  [string]$OpenClaw1Dir = 'C:\Users\13377\.openclaw',
  [string]$OpenClaw2Dir = 'D:\OpenClaw2',
  [int]$TailLines = 100,
  [switch]$NoTail
)

$ErrorActionPreference = 'Stop'

function New-EncodedCommand([string]$commandText) {
  $bytes = [System.Text.Encoding]::Unicode.GetBytes($commandText)
  [Convert]::ToBase64String($bytes)
}

if (-not (Test-Path -LiteralPath $OpenClaw1Dir)) {
  throw "OpenClaw1Dir not found: $OpenClaw1Dir"
}
if (-not (Test-Path -LiteralPath $OpenClaw2Dir)) {
  throw "OpenClaw2Dir not found: $OpenClaw2Dir"
}

$logRoot = Join-Path $PSScriptRoot 'openclaw-logs'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

$instances = @(
  @{
    Name = 'openclaw1'
    Dir = $OpenClaw1Dir
    Entry = 'gateway.cmd'
  },
  @{
    Name = 'openclaw2'
    Dir = $OpenClaw2Dir
    Entry = 'run.bat'
  }
)

$procs = @()
$tailProcs = @()

foreach ($inst in $instances) {
  $name = $inst.Name
  $dir = $inst.Dir
  $entry = $inst.Entry

  $stdout = Join-Path $logRoot "$name.stdout.log"
  $stderr = Join-Path $logRoot "$name.stderr.log"

  ("==== {0} starting {1} ====" -f (Get-Date -Format o), $name) | Add-Content -LiteralPath $stdout -Encoding utf8
  ("==== {0} starting {1} ====" -f (Get-Date -Format o), $name) | Add-Content -LiteralPath $stderr -Encoding utf8

  $proc = Start-Process `
    -FilePath 'cmd.exe' `
    -ArgumentList @('/c', $entry) `
    -WorkingDirectory $dir `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -NoNewWindow `
    -PassThru

  $procs += [pscustomobject]@{
    Name = $name
    Process = $proc
    Stdout = $stdout
    Stderr = $stderr
  }

  Write-Host ("Started {0} (PID {1})" -f $name, $proc.Id)
}

foreach ($p in $procs) {
  Write-Host ("Stdout: [{0}] {1}" -f $p.Name, $p.Stdout)
  Write-Host ("Stderr: [{0}] {1}" -f $p.Name, $p.Stderr)
}

if ($NoTail) {
  Write-Host "NoTail enabled; exiting without log tail."
  exit 0
}

foreach ($p in $procs) {
  $name = $p.Name
  $stdout = $p.Stdout
  $stderr = $p.Stderr

  $tailOutCmd = @'
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -LiteralPath '{0}' -Tail {1} -Wait | ForEach-Object {{ "[{2}][out] $($_)" }}
'@ -f ($stdout.Replace("'", "''")), $TailLines, $name

  $tailErrCmd = @'
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -LiteralPath '{0}' -Tail {1} -Wait | ForEach-Object {{ "[{2}][err] $($_)" }}
'@ -f ($stderr.Replace("'", "''")), $TailLines, $name

  $tailProcs += Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-EncodedCommand', (New-EncodedCommand $tailOutCmd)
  ) -NoNewWindow -PassThru

  $tailProcs += Start-Process -FilePath 'powershell.exe' -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-EncodedCommand', (New-EncodedCommand $tailErrCmd)
  ) -NoNewWindow -PassThru
}

$script:cancelRequested = $false
$cancelHandler = [ConsoleCancelEventHandler]{
  param($sender, $e)
  $e.Cancel = $true
  $script:cancelRequested = $true
}
[Console]::add_CancelKeyPress($cancelHandler)

try {
  while ($true) {
    $allExited = $true
    foreach ($p in $procs) {
      $p.Process.Refresh()
      if (-not $p.Process.HasExited) { $allExited = $false }
    }
    if ($allExited) { break }
    if ($script:cancelRequested) { break }
    Start-Sleep -Milliseconds 250
  }
} finally {
  [Console]::remove_CancelKeyPress($cancelHandler)
  foreach ($tp in $tailProcs) {
    Stop-Process -Id $tp.Id -Force -ErrorAction SilentlyContinue
  }
}

if ($script:cancelRequested) {
  foreach ($p in $procs) {
    if (-not $p.Process.HasExited) {
      Stop-Process -Id $p.Process.Id -Force -ErrorAction SilentlyContinue
    }
  }
}
