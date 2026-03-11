param(
    [int]$IntervalSeconds = 30,
    [int]$RestartCooldownSeconds = 60,
    [string]$LogPath = "$env:USERPROFILE\.openclaw\logs\auto-reconnect.log",
    [switch]$DeepProbe,
    [switch]$Once
)

$ErrorActionPreference = "Stop"

function Write-Log {
    param(
        [string]$Message,
        [string]$Level = "INFO"
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] [$Level] $Message"
    Write-Host $line
    Add-Content -Path $script:LogPathResolved -Value $line
}

function Ensure-LogPath {
    $parent = Split-Path -Path $LogPath -Parent
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path -Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    if (-not (Test-Path -Path $LogPath)) {
        New-Item -ItemType File -Path $LogPath -Force | Out-Null
    }
    return (Resolve-Path -Path $LogPath).Path
}

function Get-OpenClawStatus {
    $args = @("status", "--json")
    if ($DeepProbe) {
        $args += "--deep"
    }

    $raw = (& openclaw @args 2>&1 | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($raw)) {
        throw "openclaw status returned empty output."
    }

    $jsonStart = $raw.IndexOf("{")
    if ($jsonStart -lt 0) {
        throw "Cannot locate JSON payload in openclaw status output. Raw: $raw"
    }

    $jsonText = $raw.Substring($jsonStart)
    return ($jsonText | ConvertFrom-Json)
}

function Test-OpenClawHealthy {
    param([object]$Status)

    if ($null -eq $Status) {
        return @{ healthy = $false; reason = "Status object is null." }
    }

    if ($null -eq $Status.gateway -or -not $Status.gateway.reachable) {
        return @{ healthy = $false; reason = "Gateway is not reachable." }
    }

    if ($null -ne $Status.health -and $null -ne $Status.health.ok -and -not $Status.health.ok) {
        return @{ healthy = $false; reason = "Health check reports not ok." }
    }

    if ($null -ne $Status.health -and $null -ne $Status.health.channels) {
        foreach ($channelProp in $Status.health.channels.PSObject.Properties) {
            $channelName = $channelProp.Name
            $channelValue = $channelProp.Value

            if ($channelValue.configured -and $null -ne $channelValue.probe -and -not $channelValue.probe.ok) {
                return @{ healthy = $false; reason = "Channel '$channelName' probe failed." }
            }

            if ($null -ne $channelValue.accounts) {
                foreach ($acctProp in $channelValue.accounts.PSObject.Properties) {
                    $acctName = $acctProp.Name
                    $acctValue = $acctProp.Value
                    if ($acctValue.configured -and $null -ne $acctValue.probe -and -not $acctValue.probe.ok) {
                        return @{ healthy = $false; reason = "Channel '$channelName' account '$acctName' probe failed." }
                    }
                }
            }
        }
    }

    return @{ healthy = $true; reason = "OK" }
}

function Restart-OpenClawGateway {
    Write-Log -Message "Restarting OpenClaw gateway..." -Level "WARN"

    try {
        & openclaw gateway stop | Out-Null
    }
    catch {
        Write-Log -Message "openclaw gateway stop failed: $($_.Exception.Message)" -Level "WARN"
    }

    Start-Sleep -Seconds 2

    & openclaw gateway restart | Out-Null
    Start-Sleep -Seconds 3

    $status = Get-OpenClawStatus
    $result = Test-OpenClawHealthy -Status $status
    if (-not $result.healthy) {
        throw "Health still abnormal after restart: $($result.reason)"
    }
}

if (-not (Get-Command openclaw -ErrorAction SilentlyContinue)) {
    throw "openclaw command not found in PATH."
}

$script:LogPathResolved = Ensure-LogPath
Write-Log -Message "OpenClaw monitor started. interval=${IntervalSeconds}s cooldown=${RestartCooldownSeconds}s deepProbe=$($DeepProbe.IsPresent)"

$lastRestartAt = [DateTime]::MinValue

while ($true) {
    try {
        $status = Get-OpenClawStatus
        $check = Test-OpenClawHealthy -Status $status

        if ($check.healthy) {
            Write-Log -Message "Health check passed."
        }
        else {
            $secondsSinceRestart = ([DateTime]::UtcNow - $lastRestartAt.ToUniversalTime()).TotalSeconds
            if ($secondsSinceRestart -lt $RestartCooldownSeconds) {
                Write-Log -Message ("Abnormal: {0}. Cooldown active ({1:N0}s/{2}s), skip restart." -f $check.reason, $secondsSinceRestart, $RestartCooldownSeconds) -Level "WARN"
            }
            else {
                Write-Log -Message "Abnormal: $($check.reason)" -Level "ERROR"
                Restart-OpenClawGateway
                $lastRestartAt = Get-Date
                Write-Log -Message "Gateway restart completed successfully."
            }
        }
    }
    catch {
        $secondsSinceRestart = ([DateTime]::UtcNow - $lastRestartAt.ToUniversalTime()).TotalSeconds
        if ($secondsSinceRestart -lt $RestartCooldownSeconds) {
            Write-Log -Message ("Check failed: {0}. Cooldown active ({1:N0}s/{2}s), skip restart." -f $_.Exception.Message, $secondsSinceRestart, $RestartCooldownSeconds) -Level "ERROR"
        }
        else {
            Write-Log -Message "Check failed: $($_.Exception.Message)" -Level "ERROR"
            try {
                Restart-OpenClawGateway
                $lastRestartAt = Get-Date
                Write-Log -Message "Gateway restart completed successfully after check failure."
            }
            catch {
                Write-Log -Message "Restart failed: $($_.Exception.Message)" -Level "ERROR"
            }
        }
    }

    if ($Once) {
        Write-Log -Message "Run finished because -Once was specified."
        break
    }

    Start-Sleep -Seconds $IntervalSeconds
}
