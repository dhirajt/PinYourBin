# PinYourBin               [![Build Status](https://travis-ci.org/dhirajt/PinYourBin.svg)](https://travis-ci.org/dhirajt/PinYourBin)
An android app to reduce garbage(well, an attempt really).

Development Notes
-----------------
1. Guide being used to build this app : [android-best-practices](https://github.com/futurice/android-best-practices) 

2. How to run the project locally:
   - Download [android-studio](http://developer.android.com/sdk/index.html)
   - Clone this repo and create a app/src/main/res/values/api_keys.xml file with following content
         
         ```xml
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
               <string name="GOOGLE_MAPS_API_KEY">XXX-API-KEY-HERE-XXX</string>
            </resources>
         ```
   - Get the google maps android api key from [Google Developers Console](https://console.developers.google.com/)
   - Import the cloned project into Android Studio and you're good to go!
