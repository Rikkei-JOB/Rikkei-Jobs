param(
    [string]$ApiBase = "http://localhost:8080/api/v1",
    [Parameter(Mandatory)][string]$CandidateId,
    [Parameter(Mandatory)][string]$HrToken,
    [Parameter(Mandatory)][string]$OtherHrToken,
    [Parameter(Mandatory)][string]$CandidateToken
)
$ErrorActionPreference = "Stop"
# Use candidate1 after fixture: company A may read; company B may not.
function Get-Ut03([string]$Path,[string]$Token) {
    Invoke-WebRequest -Uri "$ApiBase$Path" -Headers @{ Authorization = "Bearer $Token" } -SkipHttpErrorCheck
}
$denied = Get-Ut03 "/candidates/$CandidateId/contact" $OtherHrToken
if ([int]$denied.StatusCode -ne 403 -or ($denied.Content | ConvertFrom-Json).code -ne "CONTACT_LOCKED") {
    throw "Expected CONTACT_LOCKED for other company"
}
$before = (Get-Ut03 "/me/contact-access-logs" $CandidateToken).Content | ConvertFrom-Json
1..2 | ForEach-Object {
    $response = Get-Ut03 "/candidates/$CandidateId/contact" $HrToken
    if ([int]$response.StatusCode -ne 200) { throw "Contact read failed: $($response.StatusCode)" }
    # Do not print contact data or tokens.
}
$after = (Get-Ut03 "/me/contact-access-logs" $CandidateToken).Content | ConvertFrom-Json
if ($after.total -ne $before.total + 2) { throw "Expected exactly two new audit rows" }
Write-Output "PASS: company isolation, two explicit reads and candidate audit history."
