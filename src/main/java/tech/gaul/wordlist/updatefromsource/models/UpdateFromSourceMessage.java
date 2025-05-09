package tech.gaul.wordlist.updatefromsource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class UpdateFromSourceMessage {    
    /// The id of the wordlist to update from.
    private String id;

    /// true if the words should be updated even if they already exist.
    private boolean force;
}
