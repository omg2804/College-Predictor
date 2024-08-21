import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class CollegePredictorGUI extends JFrame {

    private List<College> colleges;
    private JComboBox<String> branchComboBox;
    private JComboBox<String> categoryComboBox;
    private JTextField jeeRankField;
    private JEditorPane predictionArea;

    public CollegePredictorGUI() {
        setTitle("College Predictor");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a gradient background panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(18, 52, 86); // Dark blue
                Color color2 = new Color(0, 113, 188); // Light blue
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        backgroundPanel.setLayout(new BorderLayout());

        getContentPane().add(backgroundPanel);

        loadCollegesData();

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10)); // Increased gaps between components
        inputPanel.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white background
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        inputPanel.add(new JLabel("JEE Rank:"));
        jeeRankField = new JTextField();
        inputPanel.add(jeeRankField);
        inputPanel.add(new JLabel("Desired Branch:"));
        branchComboBox = new JComboBox<>(new String[]{"Computer Science", "Electrical Engineering", "Mechanical Engineering", "ECE","Artificial Intelligence and Machine Learning","Data Science and Analytics"});
        inputPanel.add(branchComboBox);
        inputPanel.add(new JLabel("Category:"));
        categoryComboBox = new JComboBox<>(new String[]{"General","Gen-Female", "OBC","OBC-Fem", "SC","SC-Fem", "ST","ST-Fem", "PWD","PWD-Fem"});
        inputPanel.add(categoryComboBox);

        JButton predictButton = new JButton("Predict");
        predictButton.setBackground(new Color(243, 190, 45));
        predictButton.setForeground(Color.WHITE);
        predictButton.setFocusPainted(false);
        predictButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                predictColleges();
            }
        });

        predictionArea = new JEditorPane();
        predictionArea.setEditable(false);
        predictionArea.setContentType("text/html");
        predictionArea.setBackground(new Color(240, 240, 240, 200)); // Semi-transparent light gray background
        predictionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        predictionArea.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font style, size, and family
        predictionArea.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        backgroundPanel.add(inputPanel, BorderLayout.NORTH);
        backgroundPanel.add(new JScrollPane(predictionArea), BorderLayout.CENTER);
        backgroundPanel.add(predictButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadCollegesData() {
        colleges = new ArrayList<>();
        String url = "jdbc:mysql://localhost:3306/kpr";
        String username = "root";
        String password = "Kamlesh@6037";
        String query = "SELECT * FROM college ORDER BY Collegename ASC"; // Order by college name

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String name = resultSet.getString("Collegename");
                String branch = resultSet.getString("Department");
                String category = resultSet.getString("Category");
                int openingRank = resultSet.getInt("CutoffRankMin");
                int closingRank = resultSet.getInt("CutoffRankMax");
                int placement = resultSet.getInt("AveragePackage");
                int nirfRanking = resultSet.getInt("NIRFRank");
                String officialLink = resultSet.getString("Website");
                colleges.add(new College(name, branch, category, openingRank, closingRank, placement, nirfRanking, officialLink));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void predictColleges() {
        int jeeRank;
        try {
            jeeRank = Integer.parseInt(jeeRankField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid JEE rank.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String desiredBranch = (String) branchComboBox.getSelectedItem();
        String category = (String) categoryComboBox.getSelectedItem();

        StringBuilder htmlContent = new StringBuilder("<html><body>");
        boolean collegesFound = false;
        for (College college : colleges) {
            if (college.isOpenForAdmission(desiredBranch, category, jeeRank)) {
                htmlContent.append("<p><b>").append(college.getName()).append("</b>")
                        .append("<br> Avg. Placement: ").append(college.getPlacement())
                        .append("<br> NIRF Ranking: ").append(college.getNirfRanking())
                        .append("<br> Official Link: <a href=\"").append(college.getOfficialLink()).append("\">").append(college.getOfficialLink()).append("</a></p>");
                collegesFound = true;
            }
        }
        htmlContent.append("</body></html>");

        if (!collegesFound) {
            predictionArea.setText("No colleges found for the given criteria.");
        } else {
            predictionArea.setText(htmlContent.toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CollegePredictorGUI();
            }
        });
    }

    class College {
        private String name;
        private String branch;
        private String category;
        private int openingRank;
        private int closingRank;
        private int placement;
        private int nirfRanking;
        private String officialLink;

        public College(String name, String branch, String category, int openingRank, int closingRank, int placement, int nirfRanking, String officialLink) {
            this.name = name;
            this.branch = branch;
            this.category = category;
            this.openingRank = openingRank;
            this.closingRank = closingRank;
            this.placement = placement;
            this.nirfRanking = nirfRanking;
            this.officialLink = officialLink;
        }

        public String getName() {
            return name;
        }

        public int getPlacement() {
            return placement;
        }

        public int getNirfRanking() {
            return nirfRanking;
        }

        public String getOfficialLink() {
            return officialLink;
        }

        public boolean isOpenForAdmission(String desiredBranch, String category, int jeeRank) {
            return this.branch.equalsIgnoreCase(desiredBranch) &&
                    this.category.equalsIgnoreCase(category) &&
                    jeeRank >= openingRank &&
                    jeeRank <= closingRank;
        }
    }
}