package tech.gaul.wordlist.updatefromsource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class QueryWordMessage {
    /// The word to query
    private String word;

    /// True to update the word even if it already exists
    private boolean force;
}
