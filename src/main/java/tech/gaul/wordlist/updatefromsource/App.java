package tech.gaul.wordlist.updatefromsource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.sqs.SqsClient;
import tech.gaul.wordlist.updatefromsource.models.UpdateFromSourceMessage;
import tech.gaul.wordlist.updatefromsource.models.WordListSource;

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
    private final SqsClient sqsClient;
    private final DynamoDbEnhancedClient dynamoDbClient;    
    private final DynamoDbTable<WordListSource> wordListSourceTable;

    public App() {        
        sqsClient = DependencyFactory.sqsClient();
        dynamoDbClient = DependencyFactory.dynamoDbClient();
        wordListSourceTable = dynamoDbClient.table(
            getWordListSourceTableName(), 
            TableSchema.fromBean(WordListSource.class)
        );
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

    protected String getWordListSourceTableName() {
        return "WORD_LIST_SOURCE_TABLE_NAME";
    }

    @Override
    public Object handleRequest(final SQSEvent input, final Context context) {
        input.getRecords().forEach(record -> {
            String messageBody = record.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                UpdateFromSourceMessage sourceMessage = objectMapper.readValue(messageBody, UpdateFromSourceMessage.class);

                WordListSource source = wordListSourceTable.getItem(Key.builder().partitionValue(sourceMessage.getName()).build());

                if (source == null) {
                    context.getLogger().log("Source not found: " + sourceMessage.getName());
                    return;
                }

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
