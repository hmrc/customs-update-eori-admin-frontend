
# Customs Update EORI Admin Frontend

This frontend service updates an EORI and corresponding enrolment.

## Development

You'll need [Service Manager](https://github.com/hmrc/service-manager) to develop locally.

#### Service Manager Commands

To check what's running:

    sm -s

Start the required development services (make sure your service-manager-config folder is up to date)

    run provided `./run-services.sh` script

    or

    sm --start CUSTOMS_UPDATE_EORI_ADMIN_FRONTEND -r

Stop all running services

    sm --stop CUSTOMS_UPDATE_EORI_ADMIN_FRONTEND

## Debugging

You will need to start your local debugging session on the expected port

    sbt -jvm-debug 9999
    run 9000

    Or within an SBT Shell inside an IDE:
    run -Dlogger.customs-update-eori-admin-frontend=DEBUG 9000
    then Click the DEBUG icon “Attach debugger to sbt shell”

#### [Scoverage](https://github.com/scoverage/sbt-scoverage)

We're using Scoverage to check the code coverage of our test suites.

You can run this on the command line with

    sbt clean coverage test coverageReport

Or from with SBT using

    ; clean ; coverage ; test ; coverageReport

Adjust the following in `build.sbt` to configure Scoverage

    ...
    ScoverageKeys.coverageMinimum := 70,
    ScoverageKeys.coverageFailOnMinimum := false,
    ...

#### Running `customs-update-eori-admin-frontend` locally
    1. run `./run-services.sh` script
    2. launch `customs-update-eori-admin-frontend` via sbt using `sbt run` command
    3. set up known-facts and enrolments stubs as per examples bellow
    4. navigate to `http://localhost:9000/customs-update-eori-admin-frontend`
    5. enter any String for `PID` field and enter `update-enrolment-eori` in `Roles` text-box

#### Example known-facts test data creation
```
curl --location --request POST 'http://localhost:9595/enrolment-store-stub/known-facts' \
--header 'Content-Type: application/json' \
--data-raw '{
  "service": "HMRC-CUS-ORG",
  "knownFacts": [
    {
      "key": "EORINumber",
      "value": "GB123456789000",
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
 "groupId": "90ccf333-65d2-4bf2-a008-abc23783",
 "affinityGroup": "Organisation",
 "users": [
  {
   "credId": "0012236665",
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
     "value": "GB123456789000"
    }
   ],
   "enrolmentFriendlyName": "Customs Enrolment",
   "assignedUserCreds": [
    "0012236665"
   ],
   "state": "Activated",
   "enrolmentType": "principal",
   "assignedToAll": false
  }
 ]
}
```