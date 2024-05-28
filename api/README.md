This module contains the code for the myzappi APIs


### Discover devices
```
POST /devices/discover
Request
{
"serialNumber": "12345678",
"apiKey": "myDemoApiKey"
}

Response 202
Response 409 - if the device is already registered
```

### Unregister devices
```
DELETE /devices
Response 204
```

### Register myenergi myaccount credentials
This API reads the email and password from the body and confirms the details are correct by calling the hubs and devices API
https://myaccount.myenergi.com/api/Product/UserHubsAndDevices using the oauth client and the response should have a 200 OK
and a JSON body with the following contents. "content" may or may not be set.
```
{"status":true,"message":"","field":"","content":[]}
```
If this is successful, the email address and password are stored in a new table (myenergi_creds) in DynamoDB. It stores
the amazon user id as the primary key and the email and password as attributes. The email and password are encrypted using
KMS keys.

### Get summary of connected accounts
```
GET /account/summary API
{
    "hubRegistered": true,  // hub is registered and s18 server is configured
    "myaccountRegistered": false    // oauth configured
}
```

```
POST /account/register
{
    "email": "email",
    "password": "password"
}
```
response 200 OK
```
{
    "email": "email"
}
```

### Get Device
```
GET /devices/{serialNumber}
Response 200, 404
{
    "serialNumber": "12345678",
    "type": "zappi"
}
```
 
### List Devices               
```
GET /devices
Response 200
{
    "devices": [
        {
            "serialNumber": "12345678",
            "type": "zappi"
        }
    ]
}
```
                
```
GET /devices?type=zappi
Response 200
{
    "devices": [
        {
            "serialNumber": "12345678",
            "type": "zappi"
        }
    ]
}
```

```
GET /devices/{serialNumber}/status

Response 200
{
    "serialNumber": "12345678",
    "type": "zappi",
    "mode": "eco",
    "firmware": "1.2.3",
    
    "energy": {
        "solarGeneration": "20",
        "consumpting": "10",
        "importing": "5",
        "exporting": "2"
    },
    // zappi fields
    "chargeAddedKwh": "10",
    "lockStatus": "locked",
    "connectionStatus": "connected",
    "chargingStatus": "charging",
    "chargeRate": "5",
    
    // eddi fields
    
    
    
    
    
    
     
    "boost": {
        "status": "enabled",
        "kWh": "5",
        "duration": "10"
    }
}
```
                
### Commands
```
POST /devices/{serialNumber}/unlock
Response 202
```

#### Set mode
```
PUT /devices/{serialNumber}/mode
Request
{
    "mode": "eco"
}
Response 202
```

#### Enable boost
```
PUT /devices/{serialNumber}/boost
Request
{
    tbd
}
Response 202
```

#### Disable boost
```
DELETE /devices/{serialNumber}/boost
Response 204
```

#### Set Libbi Target Energy
```
PUT /devices/{serialNumber}/target-energy
{
    "targetEnergyWh": "10"
}
```


Schedule APIs
PUT, GET and DELETE
Single / Recurring
Single - needs date and time and timezone
Recurring - time and day of the week

Actions:
- boost until time, kWh, duration
- Set charge mode
- Reminders (configured on Alexa but can be modified and deleted here)

GET /schedules
```
{
    "schedules": [{
        "id": "1234567890",
        "type": "RECURRING",
        "startTime": "14:00",
        "zoneId": "Europe/Dublin",
        "days": [1, 3, 5],
        "action": {
            "type": "chargeMode",
            "value": "ECO+"
        }
    }, {
        "id": "1234567890",
        "type": "RECURRING",
        "startTime": "14:00",
        "zoneId": "Europe/Dublin",
        "days": [1-7],
        "action": {
            "type": "remindCost"    // alexa reminds every day the energy cost
        }
    }, {
        "id": "1234567890",
        "type": "RECURRING",
        "startTime": "14:00",
        "zoneId": "Europe/Dublin",
        "days": [1-7],
        "action": {
            "type": "remindPlugStatus"    // alexa reminds every day if the E.V is plugged in
        }
    }, {
        "id": "0987654321",
        "startTime": "15:00",
        "zoneId": "Europe/Dublin",
        "action": {
            "type": "boostKwh",
            "value": "5"
            
            // or
            "type": "boostForMin",
            "value": "10"
            // or
            "type": "boostUntil",
            "value": "14:00"  
        }
    }]
}
```

# Schedule tasks
1. API
    1. API design - done
    2. API skeleton
    3. API gateway changes
    4. EndpointRouter changes - done
    5. Controller classes
        1. Input validation
2. DB
    1. design - user-id primary key and then blob of data in the other column
    2. DB creation - done
    3. DB DAL
3. Business logic
    1. GET
    2. POST
        1. Change charge mode
        2. Set boost mode
        3. Scheduler interaction
        4. DB interaction
    3. DELETE