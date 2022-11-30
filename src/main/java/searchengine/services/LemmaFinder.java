package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class LemmaFinder {
    private final LuceneMorphology morphologyRus;
    private final LuceneMorphology morphologyEng;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "CONJ", "VERB", "INT", "PREP"};


    public static LemmaFinder getInstance() throws IOException {
        LuceneMorphology morphologyRus = new RussianLuceneMorphology();
        LuceneMorphology morphologyEng = new EnglishLuceneMorphology();
        return new LemmaFinder(morphologyRus, morphologyEng);
    }

    private LemmaFinder(LuceneMorphology luceneMorphologyRus, LuceneMorphology luceneMorphologyEng) {
        this.morphologyRus = luceneMorphologyRus;
        this.morphologyEng = luceneMorphologyEng;
    }

    private LemmaFinder() {
        throw new RuntimeException("Disallow construct");
    }

    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     *
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    public Map<String, Integer> collectLemmas(String text) {
        String[] words = splitWords(text);

        HashMap<String, Integer> lemmas = new HashMap<>();
        LuceneMorphology luceneMorphology;
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (isRussian(word)) {
                luceneMorphology = morphologyRus;
            } else {
                luceneMorphology = morphologyEng;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    private String[] splitWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private static boolean isRussian(String word) {
        return word.chars()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
}
