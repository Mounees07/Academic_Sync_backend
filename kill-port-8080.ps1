## Kill whatever is running on port 8080
## Usage: Right-click → "Run with PowerShell"  OR  run from terminal

$proc = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
        Where-Object { $_.State -eq 'Listen' } |
        Select-Object -First 1

if ($proc) {
    $procId = $proc.OwningProcess
    $name = (Get-Process -Id $procId -ErrorAction SilentlyContinue).ProcessName
    Stop-Process -Id $procId -Force
    Write-Host "✅ Killed '$name' (PID $procId) on port 8080 — port is now free." -ForegroundColor Green
} else {
    Write-Host "ℹ️  Nothing is running on port 8080." -ForegroundColor Cyan
}

Start-Sleep -Seconds 1
