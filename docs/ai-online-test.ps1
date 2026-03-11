param(
  [string]$BaseUrl = 'http://127.0.0.1:6099/api',
  [string]$BackendLog = 'D:\JavaPartical\easymeeting-java\logs\report4-backend-run.log'
)

$ErrorActionPreference = 'Stop'

function Get-LatestCaptchaCode {
  for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 300
    $line = Get-Content $BackendLog -Tail 120 | Where-Object { $_ -match 'code:' } | Select-Object -Last 1
    if ($line -and $line -match 'code:([A-Za-z0-9]{4})') {
      return $matches[1]
    }
  }
  throw 'Cannot read captcha code from backend log.'
}

function Get-CaptchaPair {
  $res = Invoke-RestMethod -Method Get -Uri "$BaseUrl/account/checkCode"
  $code = Get-LatestCaptchaCode
  return @{ key = $res.data.checkCodeKey; code = $code }
}

function Post-Json([string]$url, [hashtable]$body, [string]$token = $null) {
  $headers = @{}
  if ($token) { $headers['token'] = $token }
  return Invoke-RestMethod -Method Post -Uri $url -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 10)
}

$ts = Get-Date -Format 'yyyyMMddHHmmss'
$email = "ai_report4_$ts@example.com"
$password = 'Aa123456'
$nickName = "AI联调$ts"

# register + login
$cap1 = Get-CaptchaPair
$registerRes = Post-Json "$BaseUrl/account/register" @{
  email = $email
  nickName = $nickName
  password = $password
  checkCode = $cap1.code
  checkCodeKey = $cap1.key
}

$cap2 = Get-CaptchaPair
$loginRes = Post-Json "$BaseUrl/account/login" @{
  email = $email
  password = $password
  checkCode = $cap2.code
  checkCodeKey = $cap2.key
}
$token = $loginRes.data.token

# create and join meeting
$quickRes = Post-Json "$BaseUrl/meetingInfo/quickMeeting" @{
  meetingNoType = 1
  MeetingName = "AI联调会议$ts"
  joinType = 0
  joinPassword = ''
} $token
$meetingId = $quickRes.data

$joinRes = Post-Json "$BaseUrl/meetingInfo/joinMeeting" @{ videoOpen = $false; meetingId = $meetingId } $token

# case A: command mode (no model generation dependency)
$helpRes = Post-Json "$BaseUrl/ai/chat" @{ meetingId = $meetingId; message = '/help' } $token
$endRes = Post-Json "$BaseUrl/ai/chat" @{ meetingId = $meetingId; message = '/end' } $token

# case B: model mode (depends on provider/model resource)
$chatRes = Post-Json "$BaseUrl/ai/chat" @{ meetingId = $meetingId; message = '请用一句话总结当前会议状态。' } $token
$summaryRes = Post-Json "$BaseUrl/ai/summary" @{ meetingId = $meetingId } $token
$suggestRes = Post-Json "$BaseUrl/ai/suggest" @{ meetingId = $meetingId } $token
$testRes = Invoke-RestMethod -Method Get -Uri "$BaseUrl/ai/test"

# print report
[PSCustomObject]@{step='register';code=$registerRes.code;info=$registerRes.info} | Format-Table -AutoSize
[PSCustomObject]@{step='login';code=$loginRes.code;info=$loginRes.info;tokenLen=($token|ForEach-Object{$_.Length})} | Format-Table -AutoSize
[PSCustomObject]@{step='quickMeeting';code=$quickRes.code;meetingId=$meetingId} | Format-Table -AutoSize
[PSCustomObject]@{step='joinMeeting';code=$joinRes.code;info=$joinRes.info} | Format-Table -AutoSize

[PSCustomObject]@{step='chat:/help';code=$helpRes.code;success=$helpRes.data.success;type=$helpRes.data.type} | Format-Table -AutoSize
[PSCustomObject]@{step='chat:/end';code=$endRes.code;success=$endRes.data.success;actions=($endRes.data.actions -join ',')} | Format-Table -AutoSize

[PSCustomObject]@{step='chat:text';code=$chatRes.code;info=$chatRes.info;success=$chatRes.data.success} | Format-Table -AutoSize
[PSCustomObject]@{step='summary';code=$summaryRes.code;info=$summaryRes.info} | Format-Table -AutoSize
[PSCustomObject]@{step='suggest';code=$suggestRes.code;info=$suggestRes.info} | Format-Table -AutoSize
[PSCustomObject]@{step='test';code=$testRes.code;info=$testRes.info;success=$testRes.data.success} | Format-Table -AutoSize
