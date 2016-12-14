# accept-sdk-android

<img src="https://raw.githubusercontent.com/mposSVK/acceptSDK/master/docs/logo.png" alt="acceptSDK" width=240 height=95>

[![CI Status](http://img.shields.io/travis/mposSVK/acceptSDK-Android.svg)](https://travis-ci.org/mposSVK/acceptSDK-Android)
[![Version](https://jitpack.io/v/mposSVK/acceptSDK-Android.svg)](https://jitpack.io/#mposSVK/acceptSDK-Android)

## Overview
The library enables cashless payment processing with selected mPOS terminals (magStripe and Chip and PIN) via the fully-licensed Wirecard Bank, Wirecard Retail Services which allows acceptance of many different cards, including Visa, MasterCard and American Express.

Check out the applications using the **acceptSDK** and created as **re-branding** of **Wirecard Whitelabel solution** 

accept GER|Lexware pay|accept SGP|M1 mPOS|
-------|-----------|-------|------|
[<img style="border-radius: 25px;" src="https://lh3.googleusercontent.com/Mlm08oH9l4e-Q-QO-FQiIZaVPXo4CDNAzxZGLWR46iTWCwCmDsO4mp8Uru5tYB0LyGvF=w300-rw" alt="Accept GER" width=100 height=100>](https://play.google.com/store/apps/details?id=de.wirecard.accept.de "Accept GER")|[<img style="border-radius: 25px;" src="https://lh3.googleusercontent.com/yMuOKlGiCeNNmf5AKe87CqDX2QETD6dl8uBgU04ZvVlpHZqjSoxTqMLnjjXpwasF8Nh-=w300-rw" width=100 height=100 alt="Lexware pay">](https://play.google.com/store/apps/details?id=de.wirecard.accept.lexware "Lexware pay")|[<img  style="border-radius: 25px;" src="https://lh3.googleusercontent.com/HtgJJ8HhiupQlz2TC1FXIPHR2mXYz0ZCngg4U0FOFJL3-UaHYmyXYCdBoVRjqIRXKio=w300-rw" width=100 height=100 alt="Accept SGP">](https://play.google.com/store/apps/details?id=de.wirecard.accept.sgp "Accept SGP")|[<img  style="border-radius: 25px;" src="https://lh3.googleusercontent.com/XkPySwvwqMmj03E2gHL4WgLlANfb4zq6XN5n0mq1BqVimPh4nslFccrIcVjs4oNYmw0=w300-rw" width=100 height=100 alt="M1 mPOS">](https://play.google.com/store/apps/details?id=de.wirecard.accept.m1 "M1 mPOS")|
## Whitelabel solution
Wirecard Technologies is using the acceptSDK in their Whitelabel application which is fully integrated professional mPOS solution. The Whitelabel app is VISA and Mastercard certified and utilises the Wirecard infrastructure for card payment processing.

## Installation
There are two ways how to install the SDK.

1. Clone from GitHub and integrate the library to your project as per accept SDK - Quick Start Guide.
2. acceptSDK is available through gradle dependency
```
repositories {
	maven { url "https://jitpack.io" }
}

dependencies {
	compile 'com.github.mposSVK:acceptSDK-Android:1.5.7'
	compile 'com.fasterxml.jackson.core:jackson-databind:2.8.3'
}
```

## Extension compatibility table
SDK|Spire extension|BBPos extension|
--------|--------|--------|
1.4.10|1.4.10|1.4.10|
1.5.4|1.5.3|1.5.1|
1.5.6.3|1.5.4.1|1.5.1|
1.5.7|1.5.5|1.5.2|

## Contact
Get in touch with [acceptSDK development team](mailto://mpos-svk@wirecard.com "acceptSDK") for acceptSDK support and mPOS Whitelabel solution

Get in touch with [Wirecard mPOS retail team](mailto://retail.mpos@wirecard.com  "mpos Retails") for Wirecard payment processing services

## Documentation

All the necessary documents are available in the ./docs subfolder.

Refer to "Android Accept SDK - Quick Start Guide.pdf" for the details on how to use acceptSDK.

If using accepdSDK pod the all the required libraries are taken care of by the gradle.

## Requirements
* Device running Android > 3.0
* One of Wirecard approved terminals and handheld printers
	* BBPOS [uEMV Swiper - Chipper](http://bbpos.com/en/solutions/hardware/ "Chipper")
	* Spire [PosMate](http://www.spirepayments.com/product/posmate/ "PosMate")
	* Spire [SPm2](http://www.spirepayments.com/product/spm2/ "SPm2")
	* Datecs printer [DPP-250](http://www.datecs.bg/en/products/61 "DPP-250")

## Authors

   Wirecard Technologies Slovakia,  mpos-svk@wirecard.com 
   
