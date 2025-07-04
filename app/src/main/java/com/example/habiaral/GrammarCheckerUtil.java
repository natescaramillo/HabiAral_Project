package com.example.habiaral;

import java.util.List;
import v4.grammarchecking.threaded.GrammarChecker;
import v4.models.Suggestion;

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
}