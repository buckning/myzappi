This module contains the code for the myzappi APIs

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