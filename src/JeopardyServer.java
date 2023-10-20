// Massimo Albanese
// SOFE4790U
// Distributed Systems

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.*;


public class JeopardyServer {
    static final int HTTP_PORT = 3500;
    static final int MAX_PLAYERS = 3; // can be changed before startup
    ArrayList<Player> playerList;
    static int activePlayer;
    Questions questions = new Questions();
    Question currentQuestion;
    QuestionKey currentQuestionKey;
    int playersAnswered = 0;
    static final String CLIENT_KEY_WORD_INPUT = "CLIENT-INPUT";
    long[] answerTime = new long[MAX_PLAYERS];
    CyclicBarrier barrier = new CyclicBarrier(MAX_PLAYERS, () -> {
        playersAnswered = 0;
        currentQuestion = null;
    });

    public void run() {
//        inits the executorservice,creates a fixed thread pool based on the amount of players playing
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_PLAYERS);
        int clientId = 0;
        playerList = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(HTTP_PORT)) {
            System.out.println("Jeopardy Server listening on port " + HTTP_PORT);
            System.out.println("Waiting for " + MAX_PLAYERS + " players...");

            while (true) {
                final Socket clientSocket = serverSocket.accept();
                int threadId = clientId;
                if (playerList.size() >= MAX_PLAYERS) {
                    System.out.println("Server at capacity!");
                }
                executorService.submit(() -> {
                    try {
//                        start the game for that client
                        startGame(clientSocket, threadId);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                clientId++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.close();
        }
    }

    private void startGame(Socket client, int id) throws InterruptedException {
        try {
            DataInputStream is = new DataInputStream((client.getInputStream()));
            DataOutputStream os = new DataOutputStream(client.getOutputStream());

            synchronized (this) {
//                we enter our first synchronized block here:
//                this allows us to persist playerList across threads, and so that players who are already connected
//                to the server get notified when another player has connected

                String name = is.readUTF();
                System.out.println(name + " joined!");
                playerList.add(new Player(name, id));
                notifyAll(); // notify other threads that player has joined

                while (playerList.size() < MAX_PLAYERS) {
                    wait(); // wait until another player joins, then send notification with name
                    os.writeUTF(playerList.get(playerList.size() - 1).name + " joined!");
                }
                if (id == 0) {
                    activePlayer = id;
                    System.out.println("Game Starting!");
                }
            }

            os.writeUTF("Game Starting!");

            while (questions.anyQuestionsUnanswered()) {
                os.writeUTF(questions.toString()); // output game board
                os.writeUTF("Scores:");
                os.writeUTF(playerList.toString()); // output scores

                if (id != activePlayer) {
                    os.writeUTF("It is " + playerList.get(activePlayer).name + "'s turn!");
                }

                queryQuestion(id, os, is); // ask the activePlayer for category and score
                os.writeUTF(currentQuestion.question); // send selected question to client
                os.writeUTF(CLIENT_KEY_WORD_INPUT); // ask client for input
                queryAnswers(id, is); // get client answers
                waitForBarrier(); // thread waits here until all other threads have completed, using the CyclicBarrier
            }

            os.writeUTF("Game over!");
            os.writeUTF("Scores:");
            os.writeUTF(playerList.toString());
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new InterruptedException();
        } finally {
            try {
                client.close();
                System.out.println("Client " + id + " Closed at " + LocalTime.now());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForBarrier() throws InterruptedException {
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
            throw new InterruptedException();
        }
    }

    private void queryQuestion(int id, DataOutputStream os, DataInputStream is) throws IOException, InterruptedException {
//        ask the activePlayer for the next question by category and score
        synchronized (this) {
//            synchronized block over this, so all threads complete this once in order
            if (id == activePlayer) {
                Category cat;
                Score scr;
                while (true) {
                    cat = queryCategory(os, is);
                    scr = queryScore(os, is);
                    try {
                        currentQuestion = questions.getQuestion(cat, scr);
                        currentQuestionKey = new QuestionKey(cat, scr);
                        System.out.println(playerList.get(id).name + " chose " + currentQuestionKey);
                        notifyAll();
                        break;
                    } catch (NoSuchFieldException e) {
//                        thrown when the question has already been answered
                        os.writeUTF("Question already answered!:");
                    }
                }
            } else {
                while (currentQuestion == null) {
                    wait();
                }
            }
        }
    }

    private static Category queryCategory(DataOutputStream os, DataInputStream is) throws IOException {
//        asks the activePlayer for the category, and validates input
        Category cat;
        while (true) {
            os.writeUTF("Enter a category (e.g. " + Category.values()[0].toString().toLowerCase() + "):");
            os.writeUTF(CLIENT_KEY_WORD_INPUT);
            String s = is.readUTF();
            try {
                cat = getCategory(s);
                break;
            } catch (NoSuchFieldException e) {
                os.writeUTF("Not a valid category!");
            }
        }
        return cat;
    }

    private static Score queryScore(DataOutputStream os, DataInputStream is) throws IOException {
//        asks the activePlayer for score, and validates input
        Score scr;
        while (true) {
            os.writeUTF("Enter a score (e.g. 100):");
            os.writeUTF(CLIENT_KEY_WORD_INPUT);
            String s = is.readUTF();
            try {
                scr = getScore(s);
                break;
            } catch (NoSuchFieldException e) {
                os.writeUTF("Not a valid score!");
            }
        }
        return scr;
    }

    private void queryAnswers(int id, DataInputStream is) throws IOException {
//        asks the player for answer
        String s = is.readUTF();
        synchronized (this) {
            answerTime[id] = System.currentTimeMillis(); // tracks time of answer, so first player with right answer wins

            if (s.equals(currentQuestion.answer)) {
//                only the first player with right answer gets the score
                System.out.println(playerList.get(id).name + " got the right answer!");

                // If the activePlayer hasn't answered correctly yet, or if this player answered before the current activePlayer
                if (!currentQuestion.answer.equals(playerList.get(activePlayer).lastAnswer) || answerTime[id] < answerTime[activePlayer]) {
                    activePlayer = id;
                    playerList.get(id).score += currentQuestionKey.score.score;

                }
            } else {
//                if no player gets it right, no one gets points, question is removed, activePlayer stays the same
                System.out.println(playerList.get(id).name + " got the wrong answer!");
            }
            playerList.get(id).lastAnswer = s;
            questions.setQuestionAnswered(currentQuestionKey, true);
            playersAnswered++;
        }
    }

    static Category getCategory(String s) throws NoSuchFieldException {
//        validates category string input
        for (Category category : Category.values()) {
            if (category.name().equals(s.toUpperCase())) {
                return category;
            }
        }
        throw new NoSuchFieldException();
    }

    static Score getScore(String s) throws NoSuchFieldException {
//          validates score string input
        for (Score score : Score.values()) {
            if (String.valueOf(score.score).equals(s.toUpperCase())) {
                return score;
            }
        }
        throw new NoSuchFieldException();
    }

    public static void main(String[] argv) {
        JeopardyServer js = new JeopardyServer();
        js.run();
    }
}

class Player {
    String lastAnswer;
    String name;
    int score;
    int id;
    boolean active;

    Player(String name, int id) {
        this.name = name;
        this.id = id;
        this.score = 0;
        this.active = false;
    }

    @Override
    public String toString() {
        return name + " " + score;
    }
}

