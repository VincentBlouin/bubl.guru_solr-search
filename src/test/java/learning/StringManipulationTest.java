package learning;

import org.junit.Test;

import java.util.StringTokenizer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/*
* Copyright Mozilla Public License 1.1
*/
public class StringManipulationTest {
    @Test
    public void can_get_last_word_of_sentence(){
        assertThat(
                lastWordOfSentence("Hi sir"),
                is("sir")
        );
        assertThat(
                lastWordOfSentence("Hi"),
                is("Hi")
        );
        assertThat(
                lastWordOfSentence(""),
                is("")
        );
    }

    private String lastWordOfSentence(String sentence){
        StringTokenizer tokenizer = new StringTokenizer(
                sentence,
                " "
        );
        String lastWord = "";
        while(tokenizer.hasMoreTokens()){
            lastWord = tokenizer.nextToken();
        }
        return lastWord;
    }
}
