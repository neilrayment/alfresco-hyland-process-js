# Hyland Automate Process JavaScript Root Object

This project is an Alfresco Repository extension (compatible with ACS 25.x) that exposes a custom JavaScript Root Object named `hylandProcess`. It allows Alfresco scripts, including those triggered by folder rules, to **start** [Hyland Automate](https://www.hyland.com/en/solutions/products/hyland-automate) processes via a secured API call using OAuth 2.0.

The implementation follows the [Alfresco JavaScript Root Object Extension Point](https://docs.alfresco.com/content-services/latest/develop/repo-ext-points/javascript-root-objects/), making it usable in safe scripting environments like:

```
Repository > Data Dictionary > Scripts
```

## Why?

Accessing external services like Hyland Automate directly from Alfresco JavaScript typically requires unsafe `Packages.*` calls, which are **disabled by default** in the repository’s sandboxed JavaScript engine. To overcome this limitation, this project introduces a safe and Spring-managed root object: `hylandProcess`.

## Usage

Once deployed, you can invoke the Hyland Automate API from a JavaScript folder rule or script using the `startProcess()` method:

```javascript
var vars = {
    invoiceNumber: document.properties["sap:invoiceNo"],
    amount:        parseFloat(document.properties["sap:amount"] || 0),
    customFlag:    true
};

logger.log(hylandProcess.startProcess("Process_1748835392417", vars));
```



* The first argument is the process definition key defined in Hyland Automate.
* The second argument is a plain JS object (automatically converted to a JSON payload).
* Returns `true` if the process was successfully triggered (HTTP 2xx).


If you want to call a different process from the default process application set in alfresco-global.properties file
you can set the apiUrl in the javascript using the overloaded startProcess method:

```javascript
var vars = {
    invoiceNumber: document.properties["sap:invoiceNo"],
    amount:        parseFloat(document.properties["sap:amount"] || 0),
    customFlag:    true
};
var apiUrl = "https://studio.experience.hyland.com/<process-app>/rb/v1/process-instances"

logger.log(hylandProcess.startProcess(apiUrl, "Process_1748835392417", vars));
```
N.B. The same external application service user is used as the default configured in the alfresco-global.properties file


## Configuration

Add the following properties to `alfresco-global.properties`:

```properties
# OAuth token endpoint
hyland.oauth.url=https://auth.iam.experience.hyland.com/idp/connect/token

# OAuth client credentials
hyland.oauth.clientId=<client-id>
hyland.oauth.secret=<client-secret>

# Hyland process API endpoint
hyland.api.url=https://studio.experience.hyland.com/<process-app>/rb/v1/process-instances
```

Define also a process key by adding:

```properties
hyland.default.processKey=Process_Default
```

## Building

Build this project using Maven:

```bash
$ mvn clean package
$ ls target/
alfresco-hyland-process-1.0.0.jar
```

## Deploying

Deploy the resulting JAR into the Alfresco Repository WAR:

```bash
$ cp target/alfresco-hyland-process-1.0.0.jar $TOMCAT_DIR/webapps/alfresco/WEB-INF/lib
```

Then restart the Alfresco server.

## Internals

This project defines and registers two Spring beans:

1. `oauthService`
   Handles OAuth 2.0 token acquisition and caching.

2. `hylandProcessScript`
   Implements the `BaseScopableProcessorExtension` to expose the `hylandProcess` object into the JavaScript environment.