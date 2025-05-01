package tech.gaul.wordlist.updatefromsource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import tech.gaul.wordlist.updatefromsource.models.QueryWordMessage;
import tech.gaul.wordlist.updatefromsource.models.Source;

@Builder
@Getter
@Setter
public class WordListUpdater {
    private final Source source;
    private final LambdaLogger logger;
    private final SqsClient sqsClient;
    private final String queryWordQueueUrl;
    private final int batchSize;
    private final boolean forceUpdate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void sendBatch(List<String> words) throws JsonProcessingException {
        // Send the batch to SQS
        ObjectMapper objectMapper = new ObjectMapper();
        List<QueryWordMessage> messages = words.stream().map(w -> QueryWordMessage.builder()
                .word(w)
                .force(forceUpdate)
                .build())
                .collect(Collectors.toList());

        Map<String, SendMessageBatchRequestEntry> entries = messages.stream()
                .map(msg -> {
                    try {
                        return SendMessageBatchRequestEntry.builder()
                                .id(msg.getWord())
                                .messageBody(objectMapper.writeValueAsString(msg))
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(req -> req.id(), req -> req));

        // Send the items to SQS, 10 at a time (current limit for batch send)
        int batchSize = 10;
        while (!entries.isEmpty()) {
            List<SendMessageBatchRequestEntry> batch = entries.values()
                    .stream()
                    .limit(batchSize)
                    .collect(Collectors.toList());

            SendMessageBatchResponse response = sqsClient.sendMessageBatch(m -> m
                    .queueUrl(queryWordQueueUrl)
                    .entries(batch));

            // Remove successful entries from the map
            for (SendMessageBatchResultEntry resultEntry : response.successful()) {                
                entries.remove(resultEntry.id());
            }

            // TODO: Track unsuccessful entries and fail after a certain number of retries
        }

        words.clear();
    }

    public void update() throws IOException, InterruptedException {
        logger.log("Retrieving word list from source: " + source.getUrl());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.getUrl()))
                .build();

        HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
        BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body()));

        String line = "";

        List<String> words = new ArrayList<>();
        long lineCount = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (!line.isEmpty()) {
                words.add(line);
                if (words.size() >= batchSize) {
                    sendBatch(words);
                }
            }

            lineCount++;
            if (lineCount % 5000 == 0) {
                logger.log("Processed " + lineCount + " lines");
            }
        }

        reader.close();

        if (words.size() > 0) {
            sendBatch(words);
        }
    }
}
