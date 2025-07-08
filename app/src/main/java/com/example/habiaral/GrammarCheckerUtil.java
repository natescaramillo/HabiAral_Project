package com.example.habiaral;

import java.util.ArrayList;
import java.util.List;

import v4.grammarchecking.threaded.GrammarChecker;
import v4.models.Suggestion;
import v4.models.SuggestionToken;

public class GrammarCheckerUtil {

    private static GrammarChecker grammarChecker;

    public static void initChecker() {
        if (grammarChecker == null) {
            grammarChecker = new GrammarChecker();
        }
    }

    public static boolean isSentenceGrammaticallyCorrect(String sentence) {
        try {
            List<Suggestion> suggestions = grammarChecker.checkGrammar(sentence);
            return suggestions == null || suggestions.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<GrammarError> getGrammarErrors(String sentence) {
        List<GrammarError> errors = new ArrayList<>();
        try {
            List<Suggestion> suggestions = grammarChecker.checkGrammar(sentence);
            if (suggestions != null) {
                for (Suggestion s : suggestions) {
                    for (SuggestionToken token : s.getSuggestions()) {
                        int start = token.getIndex();
                        int end = start + token.getWord().length();
                        errors.add(new GrammarError(start, end));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errors;
    }
}
