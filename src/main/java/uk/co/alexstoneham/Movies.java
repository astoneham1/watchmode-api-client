package uk.co.alexstoneham;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

public class Movies {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("API_KEY");
    private static final String API_SEARCH = "https://api.watchmode.com/v1/search/?apiKey=" + API_KEY + "&search_field=name&search_value=";
    private static final String API_TITLE = "https://api.watchmode.com/v1/title/%d/details/?apiKey=" + API_KEY;

    // headers for stats panel
    private static final String[] HEADERS = {
            "Genre(s)", "Runtime", "User Score", "Critic Score",
            "Rating", "Language"
    };

    private static JFrame frame;
    private static JLabel titleLabel, subtitleLabel, imageLabel;
    private static JTextPane plotTextPane;
    private static JTextField movieInputField;
    private static JPanel infoPanel;

    public static void main(String[] args) {
        initUI();
    }

    // create UI elements
    private static void initUI() {
        frame = new JFrame("Movie Searcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        titleLabel = createLabel("Please enter a movie below", 26, true);
        subtitleLabel = createLabel("", 16, false);

        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(320, 180));
        imageLabel.setMaximumSize(new Dimension(320, 180));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        plotTextPane = new JTextPane();
        plotTextPane.setEditable(false);
        plotTextPane.setFocusable(false);
        plotTextPane.setOpaque(false);
        plotTextPane.setFont(new Font("Tahoma", Font.PLAIN, 14));
        plotTextPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        plotTextPane.setMargin(new Insets(10, 20, 10, 20));

        StyledDocument doc = plotTextPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(subtitleLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(imageLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(plotTextPane);

        infoPanel = new JPanel(new GridLayout(3, 4, 10, 5));
        infoPanel.setMaximumSize(new Dimension(800, 200));
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel bottomPanel = new JPanel();
        JLabel instructions = new JLabel("Enter movie name:");
        movieInputField = new JTextField(15);
        movieInputField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        JButton searchButton = new JButton("Search");

        // events
        searchButton.addActionListener(e -> performSearch());

        // simulate click when enter is pressed on the keyboard
        movieInputField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchButton.doClick();
                }
            }
        });

        bottomPanel.add(instructions);
        bottomPanel.add(movieInputField);
        bottomPanel.add(searchButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(infoPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // helper method to create labels
    private static JLabel createLabel(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Tahoma", bold ? Font.BOLD : Font.PLAIN, size));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static void performSearch() {
        // clear the panel of any previous elements
        infoPanel.removeAll();
        // format the search term
        String searchTerm = movieInputField.getText().trim().replace(" ", "%20");

        if (searchTerm.isEmpty()) return;

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_SEARCH + searchTerm)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            JSONArray results = json.getJSONArray("title_results");

            List<String[]> movieDetails = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                String id = String.valueOf(item.getInt("id"));
                String name = item.getString("name");
                String year = item.isNull("year") ? "" : String.valueOf(item.getInt("year"));
                movieDetails.add(new String[]{id, name, year});
            }

            String[] displayChoices = movieDetails.stream()
                    .map(detail -> detail[1] + " (" + detail[2] + ")")
                    .toArray(String[]::new);

            // popup screen with all the search results
            String selected = (String) JOptionPane.showInputDialog(
                    frame, "Select a movie", "Search Result",
                    JOptionPane.PLAIN_MESSAGE, null,
                    displayChoices, displayChoices[0]
            );

            if (selected == null) return;

            String selectedName = selected.substring(0, selected.lastIndexOf("(")).trim();
            int selectedYear = Integer.parseInt(selected.substring(selected.length() - 5, selected.length() - 1));

            int movieId = movieDetails.stream()
                    .filter(d -> d[1].equals(selectedName) && d[2].equals(String.valueOf(selectedYear)))
                    .mapToInt(d -> Integer.parseInt(d[0]))
                    .findFirst()
                    .orElseThrow();

            fetchAndDisplayMovieDetails(movieId, selectedName);
            movieInputField.setText("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Could not find movie/tv series. Please try again",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void fetchAndDisplayMovieDetails(int movieId, String name) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(API_TITLE, movieId)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject movieJSON = new JSONObject(response.body());

        titleLabel.setText(name);

        String type = switch (movieJSON.getString("type")) {
            case "tv_series" -> "TV Series";
            case "movie" -> "Movie";
            default -> "";
        };

        StringBuilder subtitle = new StringBuilder(type + " • " + movieJSON.getInt("year"));
        if (!movieJSON.isNull("end_year")) {
            subtitle.append(" - ").append(movieJSON.getInt("end_year"));
        }
        subtitleLabel.setText(subtitle.toString());

        String imageUrl = movieJSON.getString("backdrop");
        Image image = ImageIO.read(new URL(imageUrl));
        imageLabel.setIcon(new ImageIcon(image.getScaledInstance(320, 180, Image.SCALE_SMOOTH)));

        plotTextPane.setText(movieJSON.optString("plot_overview", "No description available."));

        String[] values = new String[HEADERS.length];

        StringBuilder genreNames = new StringBuilder();
        JSONArray genres = movieJSON.optJSONArray("genre_names");
        if (genres != null && !genres.isEmpty()) {
            for (int i = 0; i < genres.length(); i++) {
                genreNames.append(genres.optString(i, ""));
                if (i < genres.length() - 1) {
                    genreNames.append(", ");
                }
            }
        }
        values[0] = !genreNames.isEmpty() ? genreNames.toString() : "N/A";

        int runtime = movieJSON.optInt("runtime_minutes", -1);
        values[1] = runtime > 0 ? runtime + " minutes" : "N/A";

        int userRating = movieJSON.optInt("user_rating", -1);
        values[2] = userRating >= 0 ? userRating + "/10" : "N/A";

        int criticScore = movieJSON.optInt("critic_score", -1);
        values[3] = criticScore >= 0 ? criticScore + "/100" : "N/A";

        String usRating = movieJSON.optString("us_rating", "");
        values[4] = usRating.isEmpty() ? "N/A" : usRating;

        String language = movieJSON.optString("original_language", "");
        values[5] = language.isEmpty() ? "N/A" : language.toUpperCase();

        for (int i = 0; i < HEADERS.length; i++) {
            infoPanel.add(createInfoLabel(HEADERS[i], true));
            infoPanel.add(createInfoLabel(values[i], false));
        }

        infoPanel.revalidate();
        infoPanel.repaint();
    }

    private static JLabel createInfoLabel(String text, boolean isHeader) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Tahoma", isHeader ? Font.BOLD : Font.PLAIN, 12));
        return label;
    }
}