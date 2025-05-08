package uk.co.alexstoneham;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
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
        JFrame frame = new JFrame("Movie Searcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);

        JLabel titleLabel = new JLabel("Please enter a movie below");
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 24));

        JPanel topPanel = new JPanel();
        topPanel.add(titleLabel);

        // bottom panel
        JLabel instructions = new JLabel("Enter movie name:");
        JTextField countryInputField = new JTextField(15); // Width of 15 columns
        countryInputField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        JButton newCountryButton = new JButton("Search");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(instructions);
        bottomPanel.add(countryInputField);
        bottomPanel.add(newCountryButton);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        // Add a key listener to simulate button click when Enter is pressed
        countryInputField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    newCountryButton.doClick(); // Simulates a button click
                }
            }
        });


        // TO SEARCH FOR MOVIES
        newCountryButton.addActionListener(e -> {
            String filmToSearch = countryInputField.getText().trim();

            if (!filmToSearch.isEmpty()) {
                try {
                    // create api request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE_SEARCH + apiKey + API_END_SEARCH + filmToSearch))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JSONObject json = new JSONObject(response.body());

                    System.out.println(API_BASE_SEARCH + apiKey + API_END_SEARCH + filmToSearch);
                    System.out.println(json);

                    JSONArray results = json.getJSONArray("title_results");
                    String[][] movieDetails = new String[results.length()][3];

                    // collate details about the search results
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        movieDetails[i][0] = String.valueOf(item.getInt("id"));
                        movieDetails[i][1] = item.getString("name");
                        movieDetails[i][2] = String.valueOf(item.getInt("year"));
                    }

                    // create an array to display on screen including name and year of movie
                    String[] movieNamesAndYears = new String[results.length()];
                    for (int i = 0; i < results.length(); i++) {
                        movieNamesAndYears[i] = movieDetails[i][1] + " (" + movieDetails[i][2] + ")";
                    }

                    System.out.println(Arrays.toString(movieNamesAndYears));

                    // pick specific movie
                    String chosenMovieName = (String) JOptionPane.showInputDialog(
                            frame,
                            "Select a movie",
                            "Search Result",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            movieNamesAndYears,
                            movieNamesAndYears[0]
                    );

                    // cut the year out of the string
                    String chosenMovieNameShort = chosenMovieName.substring(0, chosenMovieName.length() - 7);
                    int chosenMovieYear = Integer.parseInt(chosenMovieName.substring(chosenMovieName.length() - 5, chosenMovieName.length() - 1));
                    System.out.println(chosenMovieNameShort);
                    System.out.println(chosenMovieYear);

                    int movie_id = 0;

                    // find the movie's id
                    for (String[] movieDetail : movieDetails) {
                        System.out.println(movieDetail[1]);
                        System.out.println(chosenMovieNameShort);
                        System.out.println(movieDetail[2]);
                        System.out.println(chosenMovieYear);

                        if (movieDetail[1].equals(chosenMovieNameShort) && Integer.parseInt(movieDetail[2]) == chosenMovieYear) {
                            movie_id = Integer.parseInt(movieDetail[0]);
                            System.out.println(true);
                            break;
                        }
                    }

                    // ONCE A MOVIE IS PICKED GET ALL THE DETAILS
                    HttpRequest movieRequest = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE_TITLE + movie_id + API_MIDDLE_TITLE + apiKey))
                            .build();

                    HttpResponse<String> movieResponse = client.send(movieRequest, HttpResponse.BodyHandlers.ofString());

                    JSONObject movieJSON = new JSONObject(movieResponse.body());

                    System.out.println(API_BASE_TITLE + movie_id + API_MIDDLE_TITLE + apiKey + API_END_TITLE);
                    System.out.println(movieJSON);

                    // display movie details
                    String plot = movieJSON.getString("plot_overview");
                    System.out.println(plot);
                } catch (JSONException | InterruptedException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}