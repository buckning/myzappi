## State Reconciler
The State Reconciler is responsible for ensuring that the desired state of a device matches its actual state. 
The purpose of this is to handle issues seen by users where the required state being sent to myenergi APIs is not being
honured by the device, even though the myenergi API reports success. 

This works by a command setting the desired state of a device, there is then an SQS message sent to reconcile the state
if the device is not in the desired state. The SQS queue is configured with a delay so that the device has time to process the command.
The consumer of the SQS queue (the state reconciler lambda) will then check the actual state of the device by reading it
from myenergi servers against the desired state. 

If the states do not match, the command is resent to the myenergi API to set the desired state again and another SQS message
is queued up. This process repeats until the desired state matches the actual state or until it times out (after 3 attempts).
If the states match, no further action is taken and no more SQS messages are queued.
