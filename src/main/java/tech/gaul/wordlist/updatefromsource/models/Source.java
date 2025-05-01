package tech.gaul.wordlist.updatefromsource.models;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Getter
@Setter
public class Source {
    private String name;
    private String url;

    @DynamoDbPartitionKey
    public String getName() {
        return name;
    }
}
