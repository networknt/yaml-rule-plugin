# Token-Transformer Plugin

## What does this plugin do?
A generic plugin for updating an in-flight request or response with a new/different token(s).
The plugin either retrieves new tokens from a specified token service, or are loaded from cache if not expired yet.

This plugin is used with the Light4J rule engine for loading plugins and applying actions to specified endpoint(s).

## Configuration
This section covers how you can configure the token-transformer plugin within token-transformer.yml or within values.yml under the ```token-transformer``` prefix.

### Generic Properties
Like a lot of Light4j configurations, the token-transformer has properties for setting a proxy host (```proxyHost```), proxy port (```proxyPort```),
http2 enable/disable (```enableHttp2```), and selecting what fields to mask for module registration (```moduleMasks```).
```yaml
proxyHost: <URL>
proxyPort: <Port Number>
enableHttp2: <True|False>
moduleMasks: <List Of Strings To Mask>
```

### Token Schema
The ```tokenSchema``` field contains all definitions for different types of token requests you want to use. (i.e. url-encoded, application/json, JWT construction, 2-Way-SSL, etc.).
```tokenSchema``` is a map structure where the key is used to link the schema to the defined rule engine rule.

As for the structure of each schema, they can be split into 3 different sections.

- pathPrefixAuth - Defines the auth settings for this schema, and acts as both a cache and variable store for the full plugin action.
- request - For defining how the request to the token service is made. It can also include more specific configurations for JWT construction and 2-Way-SSL.
- source - For defining how the token response gets read and where we want to save the data.
- update - For defining how the newly saved data gets used with the in-flight request/response. Most of the time, the plugin is used to update the authentication header for an in-flight request.

More detail on each section can be found below.

#### pathPrefixAuth
A Light4J configuration object that contains a number of different fields. They can be found at this link [here](https://github.com/networknt/light-4j/blob/master/config/src/main/java/com/networknt/config/PathPrefixAuth.java)
```yaml
tokenSchema:
  <tokenSchemaName>:
    pathPrefixAuth:
      <any PathPrefixAuth.class field>
    # ...
```
#### request
Request can define many different types of token requests. From simple application/json to more complex requests that include JWT construction.
The ```request``` field is a map object that has a number of different options available.

The full structure looks like the following:
```yaml
tokenSchemas:
  <tokenSchemaName>:
      # ...
      request:
        url: <String to select your token service>
        type: <Type of request ie. application/json or application/x-www-form-urlencoded>
        cacheHttpClient: <boolean to ask if you want to re-use the same http client every time.>
        jwtSchema: <OPTIONAL schema that configures JWT construction.>
        sslContextSchema: <OPTIONAL schema that configures ssl context>
        headers: <SOLVABLE map schema that contains the headers to use in the request. key = header key, value = header value>
        body: <SOLVABLE map schema that contains the body to use in the request. key = body key, value = body value>
      # ...

```

**NOTE:** Anything marked as SOLVABLE means that is can use the ```!ref(pathPrefixAuth.*)``` variables.

#### source
Source simply picks out the data needed from the token response.
The ```source``` field is map object that has two different options available. One for header definitions, and another for body definitions.

Each header and body definition contains one key to define the source (what field in the response to grab from), and another for a destination (where to save the data).

The full structure looks like the following:
```yaml
tokenSchemas:
  <tokenSchemaName>:
      # ...
      source:
        body: <A list of objects that define the source (what body key in the response) and destination (where to save the data).>
        headers: <a list of objects that define the source (what header key in the response) and the destination (where to save the data).>
      # ...
```

**NOTE:** Destinations have to use the ```!ref(pathPrefixAuth.*)``` field name.

#### update
Define what fields you want to update in the in-flight request/response.
The ```update``` field is a map object containing 3 fields. One to define the direction (REQUEST|RESPONSE), one to define the headers, and another to define the body.

The full structure looks like the following:
```yaml
tokenSchemas:
  <tokenSchemaName>:
    # ...
    update:
      direction: <REQUEST|RESPONSE>
      headers: <SOLVABLE map object containing key value pairs for headers>
      body: <SOLVABLE map object containing key value pairs for the body>
```

### Full Example

This example highlights all options for request, including 2-way-ssl and jwt construction

```yaml
  exampleSchema:

    # Our pathPrefix variables that contains some pre-defined data.
    pathPrefixAuth:
      clientId: "my-example-id"
      clientSecret: "!mY_P@55w0Rd_S3cR37!"                        # It is recommended to use bcrypt if storing in a config file.
      tokenTtl: 300000                                            # You should always define tokenTtl if the response does not contain an expiration field. Always in milliseconds format.

    # Define the request we are going to make to the token service.
    request:
      tokenGracePeriod: 50000                                     # Add a 50 second grace period when refreshing a token.
      cacheHttpClient: false                                      # we want to create the client everytime since we are generating a JWT for the request.
      url: "https://test.token-service.com/services/oauth2/token" # the token endpoint we will hit
      type: application/x-www-form-urlencoded                     # the type of request we are making.

      # The SSL context that will be used during the request.
      sslContextSchema:
        tlsVersion: TLSv1.3

        # truststore for our trust manager
        trustStore:
          name: client.truststore
          password: password

        # keystore for our key manager
        keyStore:
          name: client.keystore
          password: password
          keyPass: password

      # the JWT structure that will be used in our request.
      jwtSchema:
        # Give the constructed token a 300 second ttl
        jwtTtl: 300000

        # keystore used when signing the JWT.
        keyStore:
          name: jwtSign.pfx
          password: my-pass
          alias: privateKeyAlias
          keyPass: my-key-pass
        algorithm: SHA256withRSA

        # JWT headers
        # "{
        #   \"alg\": \"RS256\
        #  "}"
        jwtHeader:
          staticFields:
            alg: RS256

        # JWT body
        # "{
        #   \"iss\": \"aaabbbccc.18dddbb898fc\",
        #   \"sub\": \"apiuser.kafka@networknt.group.dev\",
        #   \"aud\": \"https://test.token-service.com\",
        #   \"exp\": \<currentTime> + <jwtTtl>"\",
        #   \"cur\": \<currentTime>"\",
        #   \"it\": \"<random uuid>\"
        #  }"
        jwtBody:
          staticFields:
            iss: aaabbbccc.18dddbb898fc
            sub: apiuser.kafka@networknt.group.dev
            aud: https://test.token-service.com
          expiryFields:
            - exp
          currentTimeFields:
            - cur
          uuidFields:
            - it

      # define the headers for our request.
      headers:
        Content-Type: application/x-www-form-urlencoded
        Accept: application/json

        # NOTE: The constructed JWT is always stored under pathPrefix access token.
        # It will get overwritten by the new access token retrieved from the token service.
        Authorization: "!ref(pathPrefixAuth.accessToken)"

      # define the body for our request.
      body:
        grant_type: "client_credentials"
        resource: "1abcabc-123123cccbbb-99d99d99"
        client_id: "!ref(pathPrefixAuth.clientId)"              # this means we will grab the clientId from pathPrefixAuth.
        client_secret: "!ref(pathPrefixAuth.clientSecret)"      # this means we will grab the client secret from pathPrefixAith.

    # Define how we grab the data from the token response
    source:

      # we are grabbing the new token and putting it into the accessToken pathPrefixAuth field.
      # a new expiration is calculated from the defined ttl in pathPrefixAuth.
      body:
        - source: access_token
          destination: "!ref(pathPrefixAuth.accessToken)"

    # Define how we are going to update the in-flight request/response.
    # In this case we are updating the request.
    update:
      direction: REQUEST

      # Add/Update the Authorization header with the new or cached token.
      headers:
        Authorization: "Bearer !ref(pathPrefixAuth.accessToken)"
```

## TODO
This section outlines the outstanding tasks left in this plugin.
- Support reading response headers from the token service.
- Allow for custom fields to be defined as variables instead of pathPrefixAuth.
- Change how we save constructed JWTs to be more intuitive rather than re-using pathPrefixAuth fields as temp storage.
- Move SSL context creation and JWT construction to HttpTokenRequestBuilder.class
- Change generic Runtime exceptions to be more specific.
- Allow more customization on time units used for ttl, grace period, expiration from token responses, etc.
