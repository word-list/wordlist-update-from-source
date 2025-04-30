# update-from-source

This project contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.

## Purpose

Retrieve words from a source (e.g. a word list on a website) and queue them via SQS for querying.

## Message Format

```json
{
    "source": "<name of source>",
    "force": true, /* true to force update all words, false to only update words that don't already exist */
}
```

## Prerequisites
- Java 1.8+
- Apache Maven
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- Docker

## Development

The generated function handler class just returns the input. The configured AWS Java SDK client is created in `DependencyFactory` class and you can 
add the code to interact with the SDK client based on your use case.

#### Building the project
```
mvn clean install
```

#### Testing it locally
```
sam local invoke
```

#### Adding more SDK clients
To add more service clients, you need to add the specific services modules in `pom.xml` and create the clients in `DependencyFactory` following the same 
pattern as sqsClient.

## Deployment

The generated project contains a default [SAM template](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html) file `template.yaml` where you can 
configure different properties of your lambda function such as memory size and timeout. You might also need to add specific policies to the lambda function
so that it can access other AWS resources.

To deploy the application, you can run the following command:

```
sam deploy --guided
```

See [Deploying Serverless Applications](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-deploying.html) for more info.

## Notes

Created using:

```
mvn archetype:generate -DarchetypeGroupId=software.amazon.awssdk -DarchetypeArtifactId=archetype-lambda -DarchetypeVersion=2.31.0
```