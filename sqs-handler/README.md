# Scheduler Handler
This module handles requests from scheduled sources, like eventbridge scheduler. 
It was initially intended to be used by SQS but SQS has limitations in terms of the length
of delay times. 
Andrew was too lazy to rename this module, lambda name and associated roles, so it is still
called sqs-handler.

This module contains a handler which receives the event and handles one of 2 different cases.
1. Alexa reminders: pulls out the Alexa user from the request and then pushes an Alexa skill message for the user.
2. General MyZappi schedules: pulls out the LWA user ID and performs the requested actions on it 

The flow is:
1. User asks Alexa to remind them to plug in their car 
* Alexa myzappi skill sets up a recurring reminder for the requested time
* Alexa myzappi skill sets up a one time schedule 5 minutes before the reminder time to call back the reminder lambda (this class). The user ID is stored in the schedule body.
2. Five minutes before the time AWS eventbridge scheduler invokes this lambda
* This class is primed with the Alexa messaging client ID and secret (from environment variables) and uses this to get a token 
* The token from 2a is used to post a skill message for the user.
3. The myzappi skill gets invoked from the asynchronous request from 2b. The post skill request in myzappi will have 
the user in the request so it can read the Zappi information and delay reminders and create another schedule
again and the process repeats
