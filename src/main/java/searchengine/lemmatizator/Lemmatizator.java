package searchengine.lemmatizator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

@Slf4j
public class Lemmatizator {
    private static String regexEng = "[a-zA-Z]+";
    private static String regexRus = "[а-яёЁА-я]+";
    private static final String[] SHORT_FORMS = new String[]{"т.д", "т.п.", "т.е."};
    private static final String[] PARTICLE_NAMES = new String[]{"МЕЖД", "ПРЕД", "СОЮЗ", "ЧАСТ", "вопр"};
    private static final String WORD_SEPARATORS = "[^А-ЯЁёа-я-]";
            //"\\s*(\\s|,|:;|\\?|–|—|\\[|]|\\{|}|«|»|'|'|`|\"|!|\\.|\\(|\\))\\s*";
    public HashMap<String, Integer> createLemmas(String text) throws IOException
    {
        HashMap<String, Integer> lemmas = new HashMap<>();
        text = removeHTMLTags(text);
        text = removeShortForms(text.toLowerCase());
        String[] splitText = text
                .replaceAll(WORD_SEPARATORS, " ")
                .trim().split("\\s+");
        List<String> baseForms = getWordsBaseForms(splitText);
        Set<String> finalSet = new HashSet<>(baseForms);
        String txt = makeText(baseForms);
        finalSet.forEach(el -> {
            int count = StringUtils.countMatches(txt, el);
            lemmas.put(el, count);
        });
        return lemmas;
    }

    public List<String> getLemmasOfWord(String word) {
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return luceneMorph.getNormalForms(word);
    }

    private List<String> getWordsBaseForms(String[] words) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> baseForms = new ArrayList<>(Arrays.asList(words));
        List<String> normalForms = new ArrayList<>();
        baseForms.forEach(w -> normalForms
                .addAll(luceneMorph.getNormalForms(w.toLowerCase(Locale.ROOT))));
        normalForms.removeIf(form -> !isCorrectForm(form, luceneMorph));
        return normalForms;
    }

    private boolean isCorrectForm(String word, LuceneMorphology luceneMorph)
    {
        List<String> wordInfo = luceneMorph.getMorphInfo(word);
        for (String morph : wordInfo) {
            if (morphContains(morph)) {
                return false;
            }
        }
        return true;
    }

    private boolean morphContains(String morph) {
        for (String str : PARTICLE_NAMES) {
            if (morph.contains(str)) {
                return true;
            }
        }
        return false;
    }

    private String makeText (List<String> list) {
        StringBuilder text = new StringBuilder();
        for (String s : list) {
            text.append(s).append(" ");
        }
        return text.toString().trim();
    }

    private String removeShortForms(String text) {
        for (String el : SHORT_FORMS) {
            if (text.contains(el)) {
                text = text.replaceAll(el, "");
            }
        }
        return text;
    }

    private String removeHTMLTags(String html) {
        return Jsoup.parse(html).text();
    }
}