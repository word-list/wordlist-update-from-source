package tech.gaul.wordlist.updatefromsource.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ValidateWordsMessage {
    private Word[] words;    

    @Builder
    @Setter
    @Getter
    public static class Word {
        private String name;
        private boolean forceUpdate;
    }
}
