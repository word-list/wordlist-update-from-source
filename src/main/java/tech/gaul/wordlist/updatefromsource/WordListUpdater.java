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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.sqs.SqsClient;
import tech.gaul.wordlist.updatefromsource.models.ValidateWordsMessage;
import tech.gaul.wordlist.updatefromsource.models.WordListSource;

@Builder
@Getter
@Setter
public class WordListUpdater {
    private final WordListSource source;
    private final LambdaLogger logger;
    private final SqsClient sqsClient;
    private final String validateWordsQueueUrl;
    private final int batchSize;
    private final boolean forceUpdate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void sendBatch(List<String> words) throws JsonProcessingException {
        // Send the batch to SQS
        ObjectMapper objectMapper = new ObjectMapper();
        ValidateWordsMessage message = ValidateWordsMessage.builder()
                .words(words.stream().map(w -> ValidateWordsMessage.Word.builder()
                        .name(w)
                        .forceUpdate(forceUpdate)
                        .build()).toArray(ValidateWordsMessage.Word[]::new))
                .build();
        String messageBody = objectMapper.writeValueAsString(message);
        sqsClient.sendMessage(m -> m.queueUrl(validateWordsQueueUrl).messageBody(messageBody));
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
