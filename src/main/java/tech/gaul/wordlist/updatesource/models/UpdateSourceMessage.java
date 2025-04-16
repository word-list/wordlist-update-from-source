package tech.gaul.wordlist.updatesource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class UpdateSourceMessage {    
    private String name;
    private boolean updateIfExisting;
}
