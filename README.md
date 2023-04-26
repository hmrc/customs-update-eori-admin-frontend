
# Customs Update EORI Admin Frontend

This frontend service updates an EORI and corresponding enrolments and also cancel an EORI and corresponding enrolment. 

## API Calls:

This service interacts with [Enrolment Store Proxy Service](https://github.com/hmrc/enrolment-store-proxy), 
[Tax Enrolments Service](https://github.com/hmrc/tax-enrolments) and [Customs Data Store Service](https://github.com/hmrc/customs-data-store). 
Tables which are given below show the api requests to these services and descriptions.


#### API Calls to Enrolment Store Proxy Service

| PATH                                  | Supported Methods | Description                                                                                                                   |
|---------------------------------------|-------------------|-------------------------------------------------------------------------------------------------------------------------------|
| ```/enrolments/:enrolmentKey/users``` | GET               | ES0 Get a list of credential IDs which are assigned a particular enrolment key, sorted by principal and delegated enrolments. |
| ```enrolments/:enrolmentKey/groups``` | GET               | ES1 Get a list of group IDs which are allocated a particular enrolment key, sorted by principal and delegated enrolments.	    |
| ```/enrolments```                     | GET               | ES20 Query known facts that match the supplied query parameters described in the table below. May return one or more records depending upon the specificity of the query. API will return a maximum of 50 records.|

#### API Calls to Tax Enrolments Service

| PATH                                                           | Supported Methods | Description |
|----------------------------------------------------------------|-------------------|-------------|
| ```/tax-enrolments/enrolments/:enrolmentKey```                 | PUT               | ES6 Insert or update an assigned or unassigned Enrolment (also referred to as Known Facts)|
| ```/tax-enrolments/enrolments/:enrolmentKey```                 | DELETE            | ES7 Delete an Enrolment (also referred to as Known Facts)|
| ```/tax-enrolments/groups/:groupId/enrolments/:enrolmentKey``` | POST              | ES8 Allocates an enrolment to the given group|
| ```/tax-enrolments/groups/:groupId/enrolments/:enrolmentKey``` | DELETE            | ES9 De-allocate an Enrolment from a Group|

#### API Calls to Customs Data Store Service

| PATH                                           | Supported Methods | Description |
|------------------------------------------------|-------------------|-------------|
| ```/customs-data-store/update-eori-history```  | POST | Updates the eori history for a given EORI in the cache|

## Development

You'll need [Service Manager](https://github.com/hmrc/service-manager) to develop locally.

#### Service Manager Commands

To check what's running:

    sm -s

Start the required development services (make sure your service-manager-config folder is up to date)

    run provided `./run-services.sh` script

    or

    sm --start CUSTOMS_UPDATE_EORI_ADMIN_FRONTEND_ALL -r

Stop all running services

    sm --stop CUSTOMS_UPDATE_EORI_ADMIN_FRONTEND_ALL

## Debugging

You will need to start your local debugging session on the expected port

    sbt -jvm-debug 9999
    run 11120

    Or within an SBT Shell inside an IDE:
    run -Dlogger.customs-update-eori-admin-frontend=DEBUG 11120
    then Click the DEBUG icon “Attach debugger to sbt shell”

#### [Scoverage](https://github.com/scoverage/sbt-scoverage)

We're using Scoverage to check the code coverage of our test suites.

You can run this on the command line with

    sbt clean coverage test coverageReport

Or from with SBT using

    ; clean ; coverage ; test ; coverageReport

Adjust the following in `build.sbt` to configure Scoverage

    ...
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ...

#### Running `customs-update-eori-admin-frontend` locally
    1. run `./run-services.sh` script
    2. launch `customs-update-eori-admin-frontend` via sbt using `sbt run` command
    3. set up known-facts and enrolments stubs as per examples bellow
    4. navigate to `http://localhost:11120/manage-eori-number`
    5. enter any String for `PID` field and enter `update-enrolment-eori` in `Roles` text-box

### Test Data Creation

To create test data in enrolment-store stub, known-facts and data request should be made. There are examples down 
below for each one. These are the services can be created in stub environment: 

- HMRC-CUS-ORG
- HMRC-GVMS-ORG
- HMRC-SS-ORG
- HMRC-ESC-ORG
- HMRC-CTS-ORG

#### Example known-facts test data creation
```
curl --location --request POST 'http://localhost:9595/enrolment-store-stub/known-facts' \
--header 'Content-Type: application/json' \
--data-raw '{
    "service": "HMRC-CUS-ORG",
    "knownFacts": [
        {
            "key": "EORINumber",
            "value": "GB111111111017",
            "kfType": "identifier"
        },
        {
            "key": "DateOfEstablishment",
            "value": "03/11/1997",
            "kfType": "verifier"
        }
    ]
}
```

#### Example enrolments test data creation
```
curl --location --request POST 'http://localhost:9595/enrolment-store-stub/data' \
--header 'Content-Type: application/json' \
--data-raw '{
	"groupId": "90ccf333-65d2-4bf2-a008-01dfca70277",
	"affinityGroup": "Organisation",
	"users": [
	    {
	        "credId": "00000123467",
	        "name": "Default User",
	        "email": "default@example.com",
	        "credentialRole": "Admin",
	        "description": "User Description"
	    }
	],
	"enrolments": [
	    {
	        "serviceName": "HMRC-CUS-ORG",
	        "identifiers": [
	            {
	                "key": "EORINumber",
	                "value": "GB111111111017"
	            }
	        ],
	        "enrolmentFriendlyName": "Customs Enrolment",
	        "assignedUserCreds": [],
	        "state": "Activated",
	        "enrolmentType": "principal",
	        "assignedToAll": true
        }
    ]
}
```

## Formatting code
This frontend service uses Scalafmt, a code formatter for Scala. The formatting rules configured for this repository are defined within .scalafmt.conf. Prior to checking in any changes to this repository, please make sure all files are formatted correctly.

To apply formatting to this repository using the configured rules in .scalafmt.conf execute:

```
sbt scalafmtAll
```

To check files have been formatted as expected execute:

```
sbt scalafmtCheckAll scalafmtSbtCheck
```

To apply formatting all and check formatting and tests with coverage report execute:

```
sbt clean scalafmt test:scalafmt it:test::scalafmt coverage test it:test scalafmtCheckAll coverageReport
```
