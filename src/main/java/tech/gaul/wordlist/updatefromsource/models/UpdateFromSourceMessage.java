package tech.gaul.wordlist.updatefromsource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class UpdateFromSourceMessage {    
    /// The name of the wordlist to update
    private String name;

    /// true if the words should be updated even if they already exist
    private boolean force;
}
