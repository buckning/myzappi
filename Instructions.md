# Thank you for using My Zappi!

My Zappi is an Alexa skill that allows you to control your Zappi device by interacting with Alexa. It provides the following functionality:
* Check if your E.V. is plugged in
* Configure boost for a time period or for a total KwH
* Get a summary of your solar generation, energy import/export and the current rate of charge to your E.V.
* Get a historic energy summary of a certain day
* Change the charge mode to Eco, Eco+ or fast mode

# How does My Zappi work?
My Zappi requires you to generate an API key on the myenergi website. This API key, along with the serial number of your
Zappi need to be entered into My Zappi website. My Zappi skill in Alexa must then be linked to the same account.

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
1. Set up account linking between My Zappi on the Alexa app.

2. Log in to the [My Zappi website](https://myzappiunofficial.com) and enter your myenergi Zappi serial number and API key.

The My Zappi website will check that whatever you have entered is correct. If it accepts the API key and serial number, 
you will be able to control your Zappi device through the My Zappi Alexa Skill. 

# FAQ
## Why is there a two stage process to log in?
The myenergi API key is a long key of random alphanumeric characters. Entering this into Alexa through voice is very difficult. 
The My Zappi website was created to make it easier to copy your serial number and API key from the myenergi website and
paste it directly into the My Zappi website.

## I get a failure on the My Zappi Website, what should I do?
If you see a failure message, check that the API key and serial number are correct and try again.

## What information does My Zappi store?
My Zappi stores the myenergi serial number and API key in its database, along with your Amazon user ID.
The Amazon user ID is an Amazon generated value and does not contain any personal identifiable information about you.
Your myenergi API key is encrypted using an AWS KMS key when it is configured via the My Zappi website.

## Can I remove all information from My Zappi?
Yes! You can ask Alexa to delete all information from the My Zappi website.

## How is my data stored
You data is stored in Amazon AWS DynamoDB and is encrypted at rest. Your API key is encrypted with AWS KMS before storage.

## Where is my data stored
My Zappi stores data in AWS data centers in Ireland.
