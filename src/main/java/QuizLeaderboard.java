import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QuizLeaderboard {
    
    public static void main(String[] args) throws Exception {
        String regNo = "RA2311030030012";
        String baseUrl = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for(int poll = 0; poll < 10; poll++) {
            String url = baseUrl + "/quiz/messages?regNo=" + regNo + "&poll=" + poll;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            String body = response.body();
            if(!body.trim().startsWith("{")) {
                System.out.println("No events for poll " + poll);
                continue;
            }

            JsonNode root = mapper.readTree(body);

            for(JsonNode event : root.get("events")) {
                String roundId = event.get("roundId").asText();
                String participant = event.get("participant").asText();
                int score = event.get("score").asInt();

                String key = roundId + "-" + participant;

                if(!seen.contains(key)) {
                    seen.add(key);
                    scores.put(participant, scores.getOrDefault(participant, 0) + score);
                }
            }

            System.out.println("Complete Poll " + poll);

            if(poll<9) {
                Thread.sleep(5000); 
            }
        }

        List<Map.Entry<String,Integer>> leaderboard = new ArrayList<>(scores.entrySet());
        leaderboard.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        System.out.println("\nLeaderboard:");
        for(Map.Entry<String,Integer> entry : leaderboard) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"regNo\":\"").append(regNo).append("\",");
        json.append("\"leaderboard\":[");

        for(int i=0;i<leaderboard.size();i++) {
            Map.Entry<String,Integer> e = leaderboard.get(i);

            json.append("{");
            json.append("\"participant\":\"").append(e.getKey()).append("\",");
            json.append("\"totalScore\":").append(e.getValue());
            json.append("}");

            if(i<leaderboard.size()-1) {
                json.append(",");
            }
        }
        json.append("]}");

        HttpRequest submitRequest = HttpRequest.newBuilder()
                                        .uri(URI.create(baseUrl + "/quiz/submit"))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                                        .build();
            
        HttpResponse<String> submitResponse = client.send(submitRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nSubmission Response: ");
        System.out.println(submitResponse.body());
    }
}
