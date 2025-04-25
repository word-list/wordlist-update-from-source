package tech.gaul.wordlist.updatefromsource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class UpdateFromSourceMessage {    
    private String name;
    private boolean updateIfExisting;
}
