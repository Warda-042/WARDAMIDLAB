import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Bid class
class Bid {
    private String bidderName;
    private double amount;

    public Bid(String bidderName, double amount) {
        this.bidderName = bidderName;
        this.amount = amount;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return bidderName + " - $" + amount;
    }
}

// Validation
class BidValidator {
    public boolean isValid(Bid bid) {
        return bid.getBidderName() != null && !bid.getBidderName().trim().isEmpty() && bid.getAmount() > 0;
    }
}

// Database handling using SQLite
class Database {
    private static final String DB_URL = "jdbc:sqlite:bids.db";

    public Database() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS bids (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "bidderName TEXT NOT NULL," +
                         "amount REAL NOT NULL)";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertBid(Bid bid) {
        String sql = "INSERT INTO bids(bidderName, amount) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bid.getBidderName());
            pstmt.setDouble(2, bid.getAmount());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Bid> getAllBids() {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT bidderName, amount FROM bids";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("bidderName");
                double amount = rs.getDouble("amount");
                bids.add(new Bid(name, amount));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
}

// Repository uses database now
class BidRepository {
    private Database db = new Database();

    public void addBid(Bid bid) {
        db.insertBid(bid);
    }

    public List<Bid> getAllBids() {
        return db.getAllBids();
    }
}

// Observer pattern interface
interface BidObserver {
    void onBidPlaced(Bid bid);
}

// Controller
class BidController {
    private BidValidator validator = new BidValidator();
    private BidRepository repository = new BidRepository();
    private List<BidObserver> observers = new ArrayList<>();

    public void addObserver(BidObserver observer) {
        observers.add(observer);
    }

    public void loadBids() {
        List<Bid> bids = repository.getAllBids();
        for (Bid bid : bids) {
            for (BidObserver observer : observers) {
                observer.onBidPlaced(bid);
            }
        }
    }

    public void placeBid(String name, double amount) {
        Bid bid = new Bid(name.trim(), amount);
        if (validator.isValid(bid)) {
            repository.addBid(bid);
            for (BidObserver observer : observers) {
                observer.onBidPlaced(bid);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Invalid Bid!");
        }
    }
}


public class BidAppGUI implements BidObserver {
    private JFrame frame;
    private JTextField nameField;
    private JTextField amountField;
    private JTextArea bidDisplay;
    private BidController controller;

    public BidAppGUI() {
        controller = new BidController();
        controller.addObserver(this);
        createUI();
        controller.loadBids();  // Load existing bids from DB on startup
    }

    private void createUI() {
        frame = new JFrame("Bidding App with Database");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel(new GridLayout(5, 1));
        nameField = new JTextField();
        amountField = new JTextField();
        JButton submitBtn = new JButton("Place Bid");
        bidDisplay = new JTextArea();
        bidDisplay.setEditable(false);

        panel.add(new JLabel("Bidder Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Bid Amount:"));
        panel.add(amountField);
        panel.add(submitBtn);

        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(bidDisplay), BorderLayout.CENTER);

        submitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText();
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    controller.placeBid(name, amount);
                    nameField.setText("");
                    amountField.setText("");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
                }
            }
        });

        frame.setVisible(true);
    }

    @Override
    public void onBidPlaced(Bid bid) {
        bidDisplay.append(bid.toString() + "\n");
    }

    public static void main(String[] args) {
        // Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new BidAppGUI());
    }
}
