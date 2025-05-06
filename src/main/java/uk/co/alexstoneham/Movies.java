package uk.co.alexstoneham;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONException;
import org.json.JSONObject;

public class Movies {
    private static final HttpClient client = HttpClient.newHttpClient();

    private static final String API_BASE_SEARCH = "https://api.watchmode.com/v1/search/?apiKey=";
    private static final String API_END_SEARCH = "&search_field=name&search_value=";

    private static final String API_BASE_TITLE = "https://api.watchmode.com/v1/title/";
    private static final String API_MIDDLE_TITLE = "/details/?apiKey=";
    private static final String API_END_TITLE = "&append_to_response=sources";

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("API_KEY");
        System.out.println(apiKey);

        // swing components

        String filmToSearch = "Inception";

        // TO SEARCH FOR MOVIES
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_SEARCH + apiKey + API_END_SEARCH + filmToSearch))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            System.out.println(API_BASE_SEARCH + apiKey + API_END_SEARCH + filmToSearch);
            System.out.println(json);

            // picked movie
            int movie_id = 1182444;

            // ONCE A MOVIE IS PICKED GET ALL THE DETAILS
            try {
                HttpRequest movieRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_TITLE + movie_id + API_MIDDLE_TITLE + apiKey + API_END_TITLE))
                        .build();

                HttpResponse<String> movieResponse = client.send(movieRequest, HttpResponse.BodyHandlers.ofString());

                JSONObject movieJSON = new JSONObject(movieResponse.body());

                System.out.println(API_BASE_TITLE + movie_id + API_MIDDLE_TITLE + apiKey + API_END_TITLE);
                System.out.println(movieJSON);
            } catch (JSONException | InterruptedException | IOException ex) {
                ex.printStackTrace();
            }
        } catch (JSONException | InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }
}