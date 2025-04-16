package tech.gaul.wordlist.updatesource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import tech.gaul.wordlist.updatesource.models.WordListSource;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lambda function entry point. You can change to use other pojo type or
 * implement
 * a different RequestHandler.
 *
 * @see <a
 *      href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda
 *      Java Handler</a> for more information
 */
public class App implements RequestHandler<SQSEvent, Object> {
    private final SqsAsyncClient sqsClient;

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be
        // reused for subsequent invocations.
        // It is initialized when the class is loaded.
        sqsClient = DependencyFactory.sqsClient();
        // Consider invoking a simple api here to pre-warm up the application, eg:
        // dynamodb#listTables
    }

    protected String getValidateWordsQueueUrl() {
        return System.getenv("VALIDATE_WORDS_QUEUE_URL");
    }

    protected int getBatchSize() {
        String batchSizeText = System.getenv("BATCH_SIZE");
        if (batchSizeText != null) {
            try {
                return Integer.parseInt(batchSizeText);
            } catch (NumberFormatException e) {
                // Ignore and use default
            }
        }
        return 250;
    }

    @Override
    public Object handleRequest(final SQSEvent input, final Context context) {
        input.getRecords().forEach(record -> {
            String messageBody = record.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                WordListSource source = objectMapper.readValue(messageBody, WordListSource.class);
                WordListUpdater updater = WordListUpdater.builder()
                    .source(source)
                    .sqsClient(sqsClient)
                    .logger(context.getLogger())
                    .validateWordsQueueUrl(getValidateWordsQueueUrl())
                    .batchSize(getBatchSize())
                    .build();
                updater.update();
            } 
            catch (JsonMappingException e) {
                context.getLogger().log("Invalid message JSON: " + e.getMessage());
                e.printStackTrace();
            } 
            catch (JsonProcessingException e) {
                context.getLogger().log("Invalid message JSON: " + e.getMessage());
                e.printStackTrace();
            }
            catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return new Object();
    }
}
