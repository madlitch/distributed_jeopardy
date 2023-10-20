// Massimo Albanese
// SOFE4790U
// Distributed Systems

import java.util.HashMap;
import java.util.Objects;

enum Category { // All Category Values
    LANDMARKS, INVENTIONS, CAPITALS
}

enum Score { // All score values
    ONE(100), TWO(200), THREE(300), FOUR(400);
    final int score;

    Score(int score) {
        this.score = score;
    }
}

public class Questions {

    static final String STRING_FORMAT_10 = "|%10s";
    HashMap<QuestionKey, Question> questions = new HashMap<>();

    Questions() {
        // Init all the questions into the map
        questions.put(new QuestionKey(Category.LANDMARKS, Score.ONE), new Question(
                "This tower is one of the most recognizable structures in the world and is located in Paris.",
                "eiffel"
        ));
        questions.put(new QuestionKey(Category.LANDMARKS, Score.TWO), new Question(
                "This ancient amphitheater located in the center of Rome could hold up to 80,000 spectators.",
                "colosseum"
        ));
        questions.put(new QuestionKey(Category.LANDMARKS, Score.THREE), new Question(
                "This U.S. landmark was a gift from France and stands on Liberty Island in New York Harbor.",
                "liberty"
        ));
        questions.put(new QuestionKey(Category.LANDMARKS, Score.FOUR), new Question(
                "This mausoleum, built in memory of an emperor's wife, is located in Agra, India.",
                "mahal"
        ));
        questions.put(new QuestionKey(Category.INVENTIONS, Score.ONE), new Question(
                "This man is credited with inventing the light bulb, although he improved upon a previous design.",
                "edison"
        ));
        questions.put(new QuestionKey(Category.INVENTIONS, Score.TWO), new Question(
                "This Scottish scientist discovered penicillin in 1928.",
                "fleming"
        ));
        questions.put(new QuestionKey(Category.INVENTIONS, Score.THREE), new Question(
                "This wireless communication device was invented by Guglielmo Marconi.",
                "radio"
        ));
        questions.put(new QuestionKey(Category.INVENTIONS, Score.FOUR), new Question(
                "This device was patented in 1846 by Elias Howe but made popular by Isaac Singer.",
                "sewing"
        ));
        questions.put(new QuestionKey(Category.CAPITALS, Score.ONE), new Question(
                "This city is the capital of Canada.",
                "ottawa"
        ));
        questions.put(new QuestionKey(Category.CAPITALS, Score.TWO), new Question(
                "Known as the \"City of Lights\", this is the capital of France.",
                "paris"
        ));
        questions.put(new QuestionKey(Category.CAPITALS, Score.THREE), new Question(
                "This South American capital is named after Saint James.",
                "santiago"
        ));
        questions.put(new QuestionKey(Category.CAPITALS, Score.FOUR), new Question(
                "Located on two continents, this city is the capital of Turkey.",
                "ankara"
        ));
    }


    boolean anyQuestionsUnanswered() {
//        checks if any of the questions are not answered
        for (Question question : questions.values()) {
            if (!question.answered) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        //    toString() override outputs the game board with the rows and columns, based on the enum
        String s = "";
        for (Category category : Category.values()) {
            s = s.concat(String.format(STRING_FORMAT_10, category.toString()));
        }
        s = s.concat("|\n");

        for (Score score : Score.values()) {
            for (Category category : Category.values()) {
                if (questions.get(new QuestionKey(category, score)).answered) {
                    s = s.concat(String.format(STRING_FORMAT_10, ""));
                } else {
                    s = s.concat(String.format(STRING_FORMAT_10, score.score));
                }
            }
            s = s.concat("|\n");
        }
        return s;
    }

    Question getQuestion(Category category, Score score) throws NoSuchFieldException {
//        gets a question from the question map by category and score
        Question q = questions.get(new QuestionKey(category, score));
        if (!q.answered) {
            return q;
        } else {
            throw new NoSuchFieldException();
        }
    }

    void setQuestionAnswered(QuestionKey qk, boolean answer) {
//        sets a question answered
        questions.get(qk).answered = answer;
    }
}

class QuestionKey {
//    custem class to function as a tuple, so we can use category and score as a key in the question hashmap
    Category category;
    Score score;

    QuestionKey(Category category, Score score) {
        this.category = category;
        this.score = score;
    }

    @Override
    public boolean equals(Object o) {
//        adds comparison ability so we can compare keys in the hashmap
        if (this == o)
            return true;
        if (!(o instanceof QuestionKey qk))
            return false;
        return category == qk.category && score == qk.score;
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, score);
    }

    @Override
    public String toString() {
        return category.name() + " " + score.score;
    }
}

class Question {
//    question class
    String question;
    String answer;
    boolean answered = false;

    Question(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}

