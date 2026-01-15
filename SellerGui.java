import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * SellerGui - Graphical User Interface for Seller Agent
 * Allows user to add books to the seller's catalogue
 * Displays books in a table with status tracking
 */
public class SellerGui extends JFrame {
    private SellerAgent myAgent;

    private JTextField titleField;
    private JTextField priceField;
    private JButton addButton;
    private JTable catalogueTable;
    private DefaultTableModel tableModel;

    /**
     * Constructor
     * @param agent The seller agent this GUI belongs to
     */
    public SellerGui(SellerAgent agent) {
        super(agent.getAgentName() + " - Book Seller");
        this.myAgent = agent;

        initComponents();
        layoutComponents();
        setupActions();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 550);
        setLocationRelativeTo(null); // Center on screen
        setMinimumSize(new Dimension(550, 500)); // Prevent too small
    }

    /**
     * Initialize GUI components
     */
    private void initComponents() {
        titleField = new JTextField(25);
        priceField = new JTextField(15);
        addButton = new JButton("Add Book");
        addButton.setPreferredSize(new Dimension(120, 30));

        // Create table model with column names
        String[] columnNames = {"Book Title", "Price", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        catalogueTable = new JTable(tableModel);
        catalogueTable.setFont(new Font("Arial", Font.PLAIN, 12));
        catalogueTable.setRowHeight(25);
        catalogueTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        catalogueTable.setSelectionBackground(new Color(184, 207, 229));

        // Set column widths
        catalogueTable.getColumnModel().getColumn(0).setPreferredWidth(300); // Title
        catalogueTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Price
        catalogueTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Status

        // Center align Price and Status columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        catalogueTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        catalogueTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
    }

    /**
     * Custom cell renderer for Status column with color coding
     */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(JLabel.CENTER);

            if (!isSelected) {
                String status = (String) value;
                if ("Sold".equals(status)) {
                    cell.setBackground(new Color(255, 200, 200)); // Light red
                    cell.setForeground(new Color(139, 0, 0)); // Dark red
                } else if ("Available".equals(status)) {
                    cell.setBackground(new Color(200, 255, 200)); // Light green
                    cell.setForeground(new Color(0, 100, 0)); // Dark green
                }
            }

            return cell;
        }
    }

    /**
     * Layout the components
     */
    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // Top panel - Agent info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        JLabel infoLabel = new JLabel("Agent: " + myAgent.getAgentName());
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.NORTH);

        // Main container for center content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Input form panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add Book to Catalogue"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title label and field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel titleLabel = new JLabel("Book Title:");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        inputPanel.add(titleLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        inputPanel.add(titleField, gbc);

        // Price label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        inputPanel.add(priceLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        inputPanel.add(priceField, gbc);

        // Add button
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(addButton, gbc);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Catalogue display panel with table
        JPanel cataloguePanel = new JPanel();
        cataloguePanel.setLayout(new BorderLayout(5, 5));
        cataloguePanel.setBorder(BorderFactory.createTitledBorder("Current Catalogue"));

        JScrollPane scrollPane = new JScrollPane(catalogueTable);
        scrollPane.setPreferredSize(new Dimension(550, 250));
        cataloguePanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(cataloguePanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Setup action listeners
     */
    private void setupActions() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBookToCatalogue();
            }
        });

        // Allow Enter key in price field to add book
        priceField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBookToCatalogue();
            }
        });
    }

    /**
     * Add book to catalogue action
     */
    private void addBookToCatalogue() {
        String title = titleField.getText().trim();
        String priceStr = priceField.getText().trim();

        // Validate input
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a book title.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a price.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            priceField.requestFocus();
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceStr);
            if (price <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid positive integer for the price.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            priceField.requestFocus();
            priceField.selectAll();
            return;
        }

        // Add to agent's catalogue
        int result = myAgent.updateCatalogue(title, price);

        // Clear input fields
        titleField.setText("");
        priceField.setText("");
        titleField.requestFocus();

        // Show appropriate confirmation based on result
        if (result == 1) {
            // New book added
            JOptionPane.showMessageDialog(this,
                "Book added successfully!\n" + title + " - Price: " + price,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } else if (result == 0) {
            // Price updated for existing book
            JOptionPane.showMessageDialog(this,
                "Book already exists!\nPrice updated to: " + price,
                "Price Updated",
                JOptionPane.WARNING_MESSAGE);
        } else {
            // Book was already sold - cannot re-add
            JOptionPane.showMessageDialog(this,
                "Cannot add this book!\n'" + title + "' was already sold.",
                "Error - Book Sold",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refresh the catalogue table display
     * Called by SellerAgent when catalogue changes
     */
    public void refreshCatalogue() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Clear existing rows
                tableModel.setRowCount(0);

                // Get all books from agent
                Map<String, Integer> allBooks = myAgent.getAllBooks();

                // Add each book to the table
                for (Map.Entry<String, Integer> entry : allBooks.entrySet()) {
                    String title = entry.getKey();
                    Integer price = entry.getValue();
                    String status;
                    String priceStr;

                    if (myAgent.isBookSold(title)) {
                        status = "Sold";
                        priceStr = String.valueOf(price);
                    } else {
                        status = "Available";
                        priceStr = String.valueOf(price);
                    }

                    tableModel.addRow(new Object[]{title, priceStr, status});
                }
            }
        });
    }
}
