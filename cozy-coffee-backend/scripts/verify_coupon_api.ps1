
$BaseUrl = "http://localhost:8080"
$LoginUrl = "$BaseUrl/api/auth/login"
$CouponsUrl = "$BaseUrl/api/member/mall/coupons"

function Test-Coupons {
    param (
        [string]$Username,
        [string]$Password
    )

    Write-Host "Logging in as $Username..." -ForegroundColor Cyan
    try {
        $loginBody = @{
            username = $Username
            password = $Password
        } | ConvertTo-Json

        $loginResponse = Invoke-RestMethod -Uri $LoginUrl -Method Post -Body $loginBody -ContentType "application/json"
        
        if ($loginResponse.success) {
            $token = $loginResponse.data.token
            Write-Host "Login successful. Token acquired." -ForegroundColor Green
            
            Write-Host "Testing GET $CouponsUrl..." -ForegroundColor Cyan
            try {
                $headers = @{ "Authorization" = "Bearer $token" }
                $couponsResponse = Invoke-RestMethod -Uri $CouponsUrl -Method Get -Headers $headers
                
                if ($couponsResponse.success) {
                    Write-Host "API Call Successful!" -ForegroundColor Green
                    Write-Host "Coupons Found: $($couponsResponse.data.Count)" -ForegroundColor Yellow
                    $couponsResponse.data | Format-Table -Property id, couponCode, status, productName, value
                } else {
                    Write-Host "API returned success=false: $($couponsResponse.message)" -ForegroundColor Red
                }
            } catch {
                Write-Host "Failed to call Coupons API. Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
                Write-Host "Error Details: $($_.ErrorDetails.Message)"
                Write-Host "This likely means the endpoint does not exist (404) or the service is down." -ForegroundColor Gray
            }

        } else {
            Write-Host "Login failed: $($loginResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Login request failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Run test with a default user (you might need to adjust this)
Test-Coupons -Username "cozy_user" -Password "Password123"
