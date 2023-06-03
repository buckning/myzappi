# Thank you for using My Zappi!

My Zappi is an Alexa skill that allows you to control your Zappi device by interacting with Alexa. It provides the following functionality:
* Check if your E.V. is plugged in
* Configure boost for a time period or for a total KwH
* Get a summary of your solar generation, energy import/export and the current rate of charge to your E.V.
* Get a historic energy summary of a certain day
* Change the charge mode to Eco, Eco+ or fast mode

# How does My Zappi work?
My Zappi requires you to generate an API key on the myenergi website. This API key, along with the serial number of your
Zappi need to be entered into My Zappi in the login phase. The login phase is a one time task that needs to be done before
My Zappi can function correctly.

# What do I need for this to work?
1. Alexa device - this has been tested on standard Echo device, Echo show and through the Alexa mobile app.
2. Zappi device which you own or have access to. Note that you must be able to get access to the myenergi API key for
   the Zappi device. If you cannot get this, My Zappi will not be able to work.
3. Computer, laptop, smartphone, Echo show. Pretty much anything that allows you to navigate to a website and enter some information.

# How do I get my myenergi API key and serial number?
To get the serial number of the hub, navigate and log in to your myenergi account [here](https://myaccount.myenergi.com/).

Click on [Manage Products](https://myaccount.myenergi.com/location#products). The serial number will be listed under
the device information and will be prefixed with SN.

To generate an API key, click on **Advanced** under the device on the [Manage Products](https://myaccount.myenergi.com/location#products) page.
A pop-up will open and there will be a **Generate new API Key** button. A new pop-up will open with the API key.

# How do I login?
Logging in is a two step process:
1. Initiate the login process with your serial number through Alexa
> Alexa, ask My Zappi to login

You will then be prompted to tell Alexa your serial number. This is typically an 8 digit number and should be simple for you
to tell it to Alexa.
Alexa will then tell you your My Zappi code. Make sure to write it down because you will need it in the next step!

Don't worry if you didn't hear it correctly, you can ask for it again from Alexa using the following command:
> Alexa, ask My Zappi for my code

2. Complete the login process on the My Zappi website

Visit the [My Zappi website](https://uevnoh4hxi.execute-api.eu-west-1.amazonaws.com/default/myzappi-login). Enter your My Zappi code, generated in the previous step, into the My Zappi code box. Then copy your myenergi API key from the myenergi website and enter it in the Zappi API Key text box. Click the submit button to complete the process. This may take a few seconds to complete. Once complete, you will see a success message and you are ready to start using My Zappi on Alexa.

You can test the integration using a My Zappi command, like
> Alexa, ask My Zappi for an energy summary

# FAQ
## Why is there a two stage process to log in?
The myenergi serial number and API are different data formats, one is an 8 digit number, which is easy for you to say to
Alexa, where the API key is a long, mixed case, string of alphanumeric characters which is difficult for you to say to Alexa.
It is easier for your to copy this value from the myenergi website directly to the My Zappi website.

## Why should I not need to enter my serial number in the My Zappi website?
Putting the myenergi serial number and API key into the My Zappi website gives My Zappi enough information to interact with
the Zappi device but it does not link it back to your Alexa account. My Zappi needs to know who the Alexa (Amazon) user
is and link that user to the Zappi device. The first phase of the login process stores the Amazon user ID and the
serial number. All serial number, API key and Amazon user ID could be entered on the MyZappi website but it would be
inconvenient to try and extract your Amazon user ID from Alexa and enter it on the My Zappi website.

## What is the My Zappi code?
The My Zappi code is a temporary code that provides a simple way to link your Alexa account to your myenergi serial
number/API key. It is generated  when you login and it is only valid for one login attempt. If you enter your My Zappi
code incorrectly into the My Zappi website, you will need to logout and start the login process again.

## I get a failure on the My Zappi Website, what should I do?
If you see a failure message, ask Alexa to logout and then start the login process again.
> Alexa, ask My Zappi to logout
> Alexa, ask My Zappi to login

## What information does My Zappi store?
My Zappi stores the myenergi serial number and API key in its database, along with your Amazon user ID.
The Amazon user ID is an Amazon generated value and does not contain any personal identifiable information about you.
Your myenergi API key is encrypted using an AWS KMS key when it is configured via the My Zappi login process.

## Can I remove all information from My Zappi?
Yes! You can ask Alexa to delete all information from the My Zappi database by the following command
> Alexa, ask My Zappi to logout

Once you logout of My Zappi, it will not be able to connect to your myenergi account. You must login to My Zappi again
for it to function correctly.

## How is my data stored
You data is stored in Amazon AWS DynamoDB and is encrypted at rest. Your API key is encrypted with AWS KMS before storage.

## Where is my data stored
My Zappi stores data in AWS data centers in Ireland.
