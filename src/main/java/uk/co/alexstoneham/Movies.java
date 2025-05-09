package uk.co.alexstoneham;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("TV Show • 2011");
        titleLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(320, 180));
        imageLabel.setMaximumSize(new Dimension(320, 180));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea plotArea = new JTextArea();
        plotArea.setLineWrap(true);
        plotArea.setWrapStyleWord(true);
        plotArea.setEditable(false);
        plotArea.setFocusable(false);
        plotArea.setFont(new Font("Tahoma", Font.PLAIN, 14));
        plotArea.setOpaque(false);
        plotArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        plotArea.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding

        StyledDocument doc = new DefaultStyledDocument();
        JTextPane centeredTextPane = new JTextPane(doc);
        centeredTextPane.setText("plot");
        centeredTextPane.setEditable(false);
        centeredTextPane.setFocusable(false);
        centeredTextPane.setOpaque(false);
        centeredTextPane.setFont(new Font("Tahoma", Font.PLAIN, 14));
        centeredTextPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        JPanel plotPanel = new JPanel();
        plotPanel.setLayout(new BorderLayout());
        plotPanel.add(centeredTextPane, BorderLayout.CENTER);
        plotPanel.setPreferredSize(new Dimension(600, 75)); // Match your image width
        plotPanel.setMaximumSize(new Dimension(600, 75));
        plotPanel.setOpaque(false);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(subtitleLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(imageLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(plotPanel);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 4, 10, 5)); // 6 rows total, 4 columns
        infoPanel.setMaximumSize(new Dimension(800, 200)); // Controls total size
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Cleaner look

        String[] headers = {
                "Genre(s)", "Runtime", "User Score", "Critic Score",
                "Rating", "Language"
        };

        String[] values = {"a", "b", "c", "d", "e", "f"};

        // bottom panel
        JLabel instructions = new JLabel("Enter movie name:");
        JTextField movieInputField = new JTextField(15); // Width of 15 columns
        movieInputField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        JButton newCountryButton = new JButton("Search");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(instructions);
        bottomPanel.add(movieInputField);
        bottomPanel.add(newCountryButton);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        // Add a key listener to simulate button click when Enter is pressed
        movieInputField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    newCountryButton.doClick(); // Simulates a button click
                }
            }
        });


        // TO SEARCH FOR MOVIES
        newCountryButton.addActionListener(e -> {
            infoPanel.removeAll();
            String filmToSearch = movieInputField.getText().trim().replace(" ", "%20");

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

                        String year = item.isNull("year") ? null : String.valueOf(item.getInt("year"));
                        movieDetails[i][2] = year;
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
                    titleLabel.setText(chosenMovieNameShort);

                    String type = "";

                    if (movieJSON.getString("type").equals("tv_series")) {
                        type = "TV Series";
                    } else if (movieJSON.getString("type").equals("movie")) {
                        type = "Movie";
                    }

                    Integer startYear = movieJSON.getInt("year");
                    Integer endYear = null;
                    if (!movieJSON.isNull("end_year")) {
                        endYear = movieJSON.getInt("end_year");
                    }

                    StringBuilder subtitleText = new StringBuilder();
                    subtitleText.append(type);
                    subtitleText.append(" • ");
                    subtitleText.append(startYear);
                    if (endYear != null) {
                        subtitleText.append(" - ");
                        subtitleText.append(endYear);
                    }

                    subtitleLabel.setText(subtitleText.toString());

                        String imageUrl = movieJSON.getString("backdrop");
                        URL url = new URL(imageUrl);
                        Image image = ImageIO.read(url);
                        Image scaledImage = image.getScaledInstance(320, 180, Image.SCALE_SMOOTH); // match your label size
                        imageLabel.setIcon(new ImageIcon(scaledImage));

                    String plot = movieJSON.getString("plot_overview");
                    centeredTextPane.setText(plot);

                    StringBuilder genreNames = new StringBuilder();
                    JSONArray genres = movieJSON.getJSONArray("genre_names");
                    for (int i = 0; i < genres.length(); i++) {
                        genreNames.append(genres.getString(i));
                        if (i < genres.length() - 1) {
                            genreNames.append(", ");
                        }
                    }

                    values[0] = genreNames.toString();

                    values[1] = movieJSON.getInt("runtime_minutes") + " minutes";

                    values[2] = movieJSON.getInt("user_rating") + "/10";

                    values[3] = movieJSON.getInt("critic_score") + "/100";

                    String us_rating = movieJSON.isNull("us_rating") ? null : movieJSON.getString("year");
                    values[4] = us_rating;

                    values[5] = movieJSON.getString("original_language");

                    // Add each header + value in sequence
                    for (int i = 0; i < headers.length; i++) {
                        JLabel headerLabel = new JLabel(headers[i], SwingConstants.CENTER);
                        headerLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
                        infoPanel.add(headerLabel);

                        JLabel valueLabel = new JLabel(values[i], SwingConstants.CENTER);
                        valueLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
                        infoPanel.add(valueLabel);
                    }

                    JPanel contentPanel = new JPanel();
                    contentPanel.removeAll();
                    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
                    contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    contentPanel.add(infoPanel);
                    frame.add(contentPanel, BorderLayout.CENTER);

                    movieInputField.setText("");
                } catch (JSONException | InterruptedException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}