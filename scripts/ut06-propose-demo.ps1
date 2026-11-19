param(
    [Parameter(Mandatory=$true)][string]$RoundId,
    [Parameter(Mandatory=$true)][string]$HrToken,
    [string]$BaseUrl='http://localhost:8080/api/v1/recruitment',
    [switch]$Reschedule
)
$ErrorActionPreference='Stop'
# Generates two disjoint future slots, without creating cross-service seed records.
$start=[DateTimeOffset]::UtcNow.AddDays(2)
$slots=@(
    @{startAt=$start.ToString('o');endAt=$start.AddMinutes(30).ToString('o')},
    @{startAt=$start.AddHours(1).ToString('o');endAt=$start.AddHours(2).ToString('o')}
)
$action=if($Reschedule){'reschedule'}else{'slots'}
$url=$BaseUrl.TrimEnd('/')+'/rounds/'+[Uri]::EscapeDataString($RoundId)+'/'+$action
Invoke-RestMethod -Method Post -Uri $url -Headers @{Authorization='Bearer '+$HrToken} -ContentType 'application/json' -Body (ConvertTo-Json -InputObject $slots)
