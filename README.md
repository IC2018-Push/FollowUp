# SmartFollowup

The SmartFollowup is an intuitive example usage of [IBM Cloud Push Notifications Service](https://console.ng.bluemix.net/docs/services/mobilepush/index.html?pos=2) with the help of [Watson Tone Analyzer Service](http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/tone-analyzer.html) , [Cloud Functions](https://www.ibm.com/cloud/functions/details) and [Cloudant Service](https://cloudant.com/).

The SmartFollowup app will register a feedback on the recently purchased items to the cloudant `feedback` database. The Cloud Functions will read changes in the `feedback` and will send data to Watson Tone Analyzer. The Tone Analyzer will send back the results to Cloud Functions. By analyzing the results, Cloud Functions will fetch appropriate message from Cloudant `moods` database and construct a valid message. This message gets pushed to `IBM Push Notifications service`, and is delivered to the mobile device.

  <img src="assets/arch.png" width="700" height="400">

## Requirements

* Android 
* Android Studio
* Gradle


### Setup IBM Cloud and Cloudant.

Complete the steps:

 Go to https://console.ng.bluemix.net and Log in. Click on [Catalog](https://console.ng.bluemix.net/catalog/) on the top bar.

1. On the left pane click on `Mobile` below Apps.  Create a `Push notification service`.

   <img src="assets/push.png" width="300" height="120">

2. Create an `AppID Service`.

   <img src="assets/appid.png" width="300" height="120">

3. Click on [Catalog](https://console.ng.bluemix.net/catalog/) on the top bar. On the left pane click on `Watson` below `Services`.Create  a `Watson Tone Analyzer` Service.

    <img src="assets/tone.png" width="300" height="120">

4. Create an `Cloudant Service`.

    <img src="assets/cloudant.png" width="300" height="120">

5. Create a database named `mood` in your [Cloudant](https://cloudant.com/). In the `mood` database, create a view named `new_view` and design named `moodPick`.

6. Click the new design document you have created in step 3 and update it with the following lines. Do not have to change the `_id` and `_rev` values.

```
{
  "_id": "_design/moodPick",
  "_rev": "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "indexes": {
    "fields": {
      "analyzer": "keyword",
      "index": "function(doc) {index("default", doc._id, {"facet": true});if ((doc.mood) || (doc.mood===0)){index("doc.mood", doc.mood);}if ((doc.message) || (doc.message===0)){index("message", doc.message);}}"
    }
  },
  "views": {
    "new_view": {
      "map": "function (doc) { emit(doc.mood,doc.message);}"
    }
  },
  "language": "javascript"
}

```

7. To the updated new design document, add messages for each emotions - `Fear, Sadness, Disgust, Anger and Joy` (Watson Tone Analyzer outputs). For example,

```
{
"mood": "Joy",
"message": "thank you very much for your valuable feedback. We are extremely happy to here from you. Come back again have a wonderfull shopping experience with us."
}
```

7. In your Cloudant, create one more database named `feedback`. This will be used inAndroidiOS application

## Configure Push service

 Go to your push service and add `Android` configuration.

## Setup the Cloud Functions.

The `FeedbackAction.swift` file need the following parameters to complete the actions.

- `appId` - IBM Cloud app GUID.

- `appSecret` - IBM Cloud Push Notification service appSecret.

- `version` - This is the version of the [Tone Analyzer service](https://watson-api-explorer.mybluemix.net/apis/tone-analyzer-v3#/) .

- `cloudantUserName` - Your Cloudant username. This is for accessing your `mood` database in Cloudant.

- `cloudantPassword` - Your Cloudant password. This is for accessing your `mood` database in Cloudant.

- `cloudantPermissionKey` - Your Cloudant cloudantPermission Key. This is for accessing your `mood` database in Cloudant.

- `cloudantPermissionPassword` - Your Cloudant cloudantPermission Password. This is for accessing your `mood` database in Cloudant.


1. Open the Cloud Functions Web Editor, and create `swift` action. Replace the content of the action with `FeedbackAction.swift`

2. Create a Cloudant package binding using the [IBM Cloud Functions CLI](https://console.bluemix.net/openwhisk/learn/cli).

  ```
    bx wsk -v package bind /whisk.system/cloudant CloudantPackage -p username 'cloudantUsername' -p password 'cloudantPassword'
    -p host 'yourUserName.cloudant.com' -p dbname 'complaints'
  ```

3. Create a Cloud Functions `Trigger`.

  ```
  bx wsk trigger create yourTriggerName --feed /yourNameSpace/CloudantPackage/changes
  ```

4. Create a rule to connect your action (step 1) and trigger (step 3)

  ```
  bx wsk rule create myRule yourTriggerName yourActionName
  ```

5. Open the feedbackApp-Android app in `Android Studio`. Go to the `app -> src -> main -> res -> values -> credentials.xml` file and add values for ,

  <img src="assets/credentials.png" width="700" height="300">


Add the cloudant values of `feedback` database.

6. Do gradle build

7. Run the application and Login using AppID.

8. Register for push and write a feedback.

9. You will get push notifications as feedback response.


### License

Copyright 2017-2018 IBM Corporation

Licensed under the [Apache License, Version 2.0 (the "License")](http://www.apache.org/licenses/LICENSE-2.0.html).

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.
`