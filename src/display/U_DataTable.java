package display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import static java.lang.Integer.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Maintenance of database file member data
 *
 * @author Vladimír Župka 2016
 *
 */
public class U_DataTable extends JFrame {

    static final long serialVersionUID = 1L;

    // Path to the file containing modifications for SELECT statement
    Path selectPath;
    // Path to the file containing column list for SELECT statement
    Path columnsPath;

    String selectFileName;

    // Application parameters
    Properties properties;
    BufferedReader infile;
    BufferedWriter outfile;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding");
    final String PROP_COMMENT = "SqlRead for IBM i, © Vladimír Župka 2019";

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;

    String library;
    String db_file;
    String file_member;
    String language;
    String nullMark;
    int fontSize;
    int windowHeight;
    int windowWidth;
    int screenWidth;
    int screenHeight;
    String autoWindowSize;
    String fetchFirst;
    String charCode;

    int maxFldWidth;

    // Connection to database
    Connection conn;
    // Object doing connection
    U_ConnectDB connectDB;

    // SQL attributes
    Statement stmt; // SQL statement object
    String stmtText; // SELECT statement text
    String condition; // condition in WHERE clause
    String whereClause; // WHERE clause (WHERE + condition)
    String ordering; // value in ORDER BY clause
    String orderByClause; // ORDER BY clause (ORDER BY + ordering)
    String actualColumnList = ""; // actual list of column names for SELECT
    String allColumnList = ""; // full list of column names for SELECT
    String normalColumnList = ""; // normal list of column names for SELECT

    ArrayList<String> allColNames; // all column names
    ArrayList<Integer> allColTypes; // all column types
    ArrayList<Integer> allColSizes; // all max. sizes of columns

    String[] colNames; // result set column names
    String[] colTypes; // result set column types
    int[] colSizes; // result set max. sizes of columns (number of characters)
    String[] colPrecisions; // number of digits
    String[] colScales; // number of decimal positions

    // Graphical table attributes
    JTable jTable; // graphical table
    TableModel tableModel; // JTable data model
    TableColumn[] tc; // table column array for rendering rows
    public Object[][] rows; // rows of JTable (two-dimensional array)
    int numOfRows; // number of rows in result set
    int numOfCols; // number of columns in result set
    int allNumOfCols; // number of all columns 

    // Model for selection of rows in the table
    ListSelectionModel rowSelectionModel;
    ListSelectionModel rowIndexList;
    // Index of a table row selected (by the user or the program)
    int rowIndex;

    boolean addNewRecord = true; // flag when adding a new table row
    double cellFieldFactor; // factor for column widths in the jTable

    // Objects for SELECT modification values (WHERE, ORDER BY)
    JLabel labelWhere;
    JTextArea textAreaWhere;
    JLabel labelOrder;
    JTextArea textAreaOrder;

    // Objects for displaying the SQL statement
    JTextArea textAreaStmt;
    JPanel textAreaStmtPanel;

    // Components for building the list
    JLabel listTitle;
    JLabel listPrompt;
    JLabel message;
    JButton exitButton;
    JButton dspButton;
    JButton refreshButton;
    JButton columnsButton;

    // Containers for building the list
    JPanel titlePanel;
    JPanel listPanel;
    JScrollPane scrollPaneList;
    JScrollPane scrollPaneStmt;
    JPanel buttonPanel;
    JPanel listMsgPanel;
    JPanel globalPanel;
    Container listContentPane;
    int listWidth, listHeight;
    int globalWidth, globalHeight;

    // Localized text objects
    Locale locale;
    ResourceBundle titles;
    String titEdit, changeCell, where, order, enterData;
    ResourceBundle buttons;
    String exit, insert, edit_sel, del_sel, refresh, columns, saveData, saveReturn, _return;
    ResourceBundle locMessages;
    String noRowUpd, noRowDel, noData, dataError, sqlError, invalidValue, invalidCharset, value, tooLong, length,
            tooLongForCol, contentLoaded, colValueNull, contentNotLoaded, colNotText, connLost;

    File file = null;

    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);
    Color VERY_LIGHT_BLUE = Color.getHSBColor(0.60f, 0.05f, 0.98f);

    /**
     * Constructor
     *
     * @param connectDB
     */
    @SuppressWarnings("ConvertToTryWithResources")

    public U_DataTable(U_ConnectDB connectDB) {
        this.connectDB = connectDB;

        // Try to connect database
        this.conn = connectDB.connect();
        try {
            // Application properties
            properties = new Properties();
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();

            library = properties.getProperty("LIBRARY"); // library name
            db_file = properties.getProperty("FILE"); // file name
            file_member = properties.getProperty("MEMBER"); // member name
            language = properties.getProperty("LANGUAGE"); // local language
            charCode = properties.getProperty("CHARSET");
            windowHeight = parseInt(properties.getProperty("RESULT_WINDOW_HEIGHT"));
            windowWidth = parseInt(properties.getProperty("RESULT_WINDOW_WIDTH"));
            autoWindowSize = properties.getProperty("AUTO_WINDOW_SIZE");
            nullMark = properties.getProperty("NULL_MARK");
            fontSize = parseInt(properties.getProperty("FONT_SIZE"));
            fontSize = parseInt(properties.getProperty("FONT_SIZE"));
            // Factor to multiply cell width
            cellFieldFactor = fontSize * 0.75;
            // Max. number of rows in the result set
            fetchFirst = properties.getProperty("FETCH_FIRST");
            maxFldWidth = parseInt(properties.getProperty("MAX_FIELD_LENGTH"));

            // The first member has the same name as the file
            if (file_member.toUpperCase().equals("*FIRST")) {
                file_member = db_file;
                // But save *FIRST to properties
                properties.setProperty("MEMBER", "*FIRST");
            } else {
                // Save original member name to properties
                properties.setProperty("MEMBER", file_member.toUpperCase());
            }

            // Create a new text file in directory "paramfiles"
            outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Localization classes
        Locale currentLocale = Locale.forLanguageTag(language);
        titles = ResourceBundle.getBundle("locales.L_TitleLabelBundle", currentLocale);
        buttons = ResourceBundle.getBundle("locales.L_ButtonBundle", currentLocale);
        locMessages = ResourceBundle.getBundle("locales.L_MessageBundle", currentLocale);

        // Localized button labels
        exit = buttons.getString("Exit");
        edit_sel = buttons.getString("Edit_sel");
        refresh = buttons.getString("Refresh");
        columns = buttons.getString("Columns");
        _return = buttons.getString("Return");

        // Name and path of the text file where user modifications
        // of SELECT statement (WHERE, ORDER BY) are preserved
        selectFileName = library.toUpperCase() + "-" + db_file.toUpperCase();
        selectPath = Paths.get(System.getProperty("user.dir"), "selectfiles", selectFileName + ".sel");

        // Read the file and get values of condition (WHERE)
        // and ordering (ORDER BY)
        try {
            // Create the file with two lines (if the file does not exist)
            if (!Files.exists(selectPath)) {
                ArrayList<String> lines = new ArrayList<>();
                // The file has initially exactly two lines.
                // The first line contains a semicolon,
                // the second line is empty.
                lines.add(";");
                lines.add("");
                // Create the file from the array list "lines" with two empty lines
                Files.write(selectPath, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            // System.out.println("selectPath: " + selectPath);
            // Open the file
            BufferedReader infileSelect = Files.newBufferedReader(selectPath, Charset.forName("UTF-8"));
            // Read all (two) lines to get values for WHERE and ORDER BY clauses
            StringBuilder sb = new StringBuilder();
            String line = infileSelect.readLine();
            while (line != null) {
                sb.append(line);
                line = infileSelect.readLine();
            }
            // Split the string obtained from the file (has two parts).
            // The two values may be empty.
            String[] arr = (sb.toString()).split(";");
            condition = "";
            ordering = "";
            if (arr.length > 0) {
                condition = arr[0];
            }
            if (arr.length > 1) {
                ordering = arr[1];
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            // message.setText(colNotText);
            message.setText(exc.toString());
        }

        allColumnList = "";
        normalColumnList = "";

        try {
            // Get full column list from the metadata of the table
            DatabaseMetaData dmd = this.conn.getMetaData();
            ResultSet rs = dmd.getColumns(null, library, db_file, null);
            allNumOfCols = 0;
            allColNames = new ArrayList<>();
            allColTypes = new ArrayList<>();
            allColSizes = new ArrayList<>();

            while (rs.next()) {
                colName = rs.getString(4);
                int colType = rs.getInt(5);
                int colSize = rs.getInt(7);
                allColumnList += ", " + colName;
                allColNames.add(colName);
                allColTypes.add(colType);
                allColSizes.add(colSize);

                allNumOfCols++;
                // Omit advanced column types
                // - they are incapable to render in a cell
                //if (colType != java.sql.Types.CLOB && colType != java.sql.Types.NCLOB && colType != java.sql.Types.BLOB
                //        && colType != java.sql.Types.ARRAY) // Add column name to the full list without advanced column types
                // - they are incapable to render in a cell
                {
                    normalColumnList += ", " + colName;
                }
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String aliasStmtText;
        try {
            stmt = conn.createStatement();
            aliasStmtText = "drop alias " + file_member.toUpperCase();
            stmt.execute(aliasStmtText);
        } catch (Exception exc) {
            //exc.printStackTrace();
        }
        try {
            aliasStmtText = "create alias " + file_member.toUpperCase() + " for " + db_file.toUpperCase() + "(" + file_member + ")";
            stmt.execute(aliasStmtText);
        } catch (Exception exc) {
            //exc.printStackTrace();
        }

        // Initially, the actual column list is the normal column list
        // without columns of "advanced" types (CLOB, BLOB, ARRAY)
        actualColumnList = normalColumnList;

        columnsPath = Paths.get(System.getProperty("user.dir"), "columnfiles", selectFileName + ".col");
        if (!Files.exists(columnsPath)) {
            // Write the actual column list to the .col file for the database file (table)
            try {
                ArrayList<String> colArr = new ArrayList<>();
                colArr.add(actualColumnList);
                // Write file with columns list
                // Rewrite the existing file or create and write a new file.
                Files.write(columnsPath, colArr, StandardCharsets.UTF_8);
            } catch (IOException ioe) {
                System.out.println("write columns file: " + ioe.getLocalizedMessage());
                //ioe.printStackTrace();
            }
        }
        menuBar = new JMenuBar();
        helpMenu = new JMenu("Help");
        helpMenuItemEN = new JMenuItem("Help English");
        helpMenuItemCZ = new JMenuItem("Nápověda česky");

        helpMenu.add(helpMenuItemEN);
        helpMenu.add(helpMenuItemCZ);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar); // In macOS on the main system menu bar above, in Windows on the window menu bar  

        // Register HelpWindow menu item listener
        helpMenuItemEN.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Help English")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlUpdateUserDocEn.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        // Register HelpWindow menu item listener
        helpMenuItemCZ.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Nápověda česky")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlUpdateUserDocCz.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        // End of constructor
    }

    /**
     * Build the window with the table rows (list)
     *
     * @param condition
     * @param ordering
     */
    protected void buildListWindow(String condition, String ordering) {
        this.condition = condition;
        this.ordering = ordering;

        // Construct WHERE and ORDER BY clauses if applicable
        if (!condition.isEmpty()) {
            whereClause = " WHERE " + condition;
        } else {
            whereClause = "";
        }
        if (!ordering.isEmpty()) {
            orderByClause = " ORDER BY " + ordering;
        } else {
            orderByClause = "";
        }

        // Start building the window
        // -------------------------
        textAreaWhere = new JTextArea(condition);
        textAreaOrder = new JTextArea(ordering);
        textAreaStmt = new JTextArea();

        exitButton = new JButton(exit);
        exitButton.setMinimumSize(new Dimension(70, 35));
        exitButton.setMaximumSize(new Dimension(70, 35));
        exitButton.setPreferredSize(new Dimension(70, 35));

        dspButton = new JButton(edit_sel);
        dspButton.setMinimumSize(new Dimension(130, 35));
        dspButton.setMaximumSize(new Dimension(130, 35));
        dspButton.setPreferredSize(new Dimension(130, 35));

        refreshButton = new JButton(refresh);
        refreshButton.setMinimumSize(new Dimension(140, 35));
        refreshButton.setMaximumSize(new Dimension(140, 35));
        refreshButton.setPreferredSize(new Dimension(140, 35));

        columnsButton = new JButton(columns);
        columnsButton.setMinimumSize(new Dimension(140, 35));
        columnsButton.setMaximumSize(new Dimension(140, 35));
        columnsButton.setPreferredSize(new Dimension(140, 35));

        titlePanel = new JPanel();
        listPanel = new JPanel();
        labelWhere = new JLabel();
        labelOrder = new JLabel();

        textAreaStmtPanel = new JPanel();
        listMsgPanel = new JPanel();
        buttonPanel = new JPanel();

        listTitle = new JLabel();
        listPrompt = new JLabel();
        message = new JLabel();

        textAreaStmt = new JTextArea();
        textAreaStmt.setFont(listPrompt.getFont());
        textAreaStmt.setEditable(false);
        textAreaStmt.setBackground(titlePanel.getBackground());
        scrollPaneStmt = new JScrollPane();

        // Localized messages
        noRowUpd = locMessages.getString("NoRowUpd");
        noData = locMessages.getString("NoData");
        dataError = locMessages.getString("DataError");
        sqlError = locMessages.getString("SqlError");
        invalidValue = locMessages.getString("InvalidValue");
        invalidCharset = locMessages.getString("InvalidCharset");
        connLost = locMessages.getString("ConnLost");

        // Evaluate modifications of the SELECT statement (WHERE, ORDER BY)
        // ----------------------
        evalModifications();

        // Get database table data using SELECT statement
        // -----------------------
        message = getData();
        if (!message.getText().equals("")) {
            message.setForeground(DIM_RED); // red
            listMsgPanel.add(message);
        }

        // Create the graphic table - listPanel - which is part of the window
        // ------------------------
        createTable();

        // Continue building the window using the listPanel just created
        // ----------------------------
        listTitle = new JLabel();
        BoxLayout boxLayoutY = new BoxLayout(titlePanel, BoxLayout.Y_AXIS);
        titlePanel.setLayout(boxLayoutY);

        // Localized titles
        titEdit = titles.getString("TitEdit") + library.toUpperCase() + "/" + db_file.toUpperCase();

        // If file name differs from member name - add member name in parentheses
        if (!db_file.equalsIgnoreCase(file_member)) {
            titEdit += "(" + file_member + ")";
        }
        listTitle.setText(titEdit);
        listTitle.setFont(new Font("Helvetica", Font.PLAIN, 20));
        listTitle.setMinimumSize(new Dimension(listWidth, 20));
        listTitle.setPreferredSize(new Dimension(listWidth, 20));
        listTitle.setMaximumSize(new Dimension(listWidth, 20));
        listTitle.setAlignmentX(Box.LEFT_ALIGNMENT);
        titlePanel.add(listTitle);
        titlePanel.add(Box.createRigidArea(new Dimension(10, 10)));

        // Align the title panel
        titlePanel.setAlignmentX(Box.CENTER_ALIGNMENT);

        changeCell = titles.getString("ChangeCell");
        listPrompt.setText(changeCell);
        listPrompt.setForeground(DIM_BLUE); // Dim blue
        listPrompt.setAlignmentX(Box.LEFT_ALIGNMENT);
        titlePanel.add(listPrompt);

        // User input for WHERE condition
        where = titles.getString("Where");
        labelWhere.setText(where + refresh + ".");
        BoxLayout labelWhereLayoutX = new BoxLayout(labelWhere, BoxLayout.X_AXIS);
        labelWhere.setLayout(labelWhereLayoutX);
        labelWhere.setMinimumSize(new Dimension(listWidth, 30));
        labelWhere.setPreferredSize(new Dimension(listWidth, 30));
        labelWhere.setMaximumSize(new Dimension(listWidth, 30));
        labelWhere.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        labelWhere.setForeground(DIM_BLUE); // blue
        labelWhere.setAlignmentX(LEFT_ALIGNMENT);

        BoxLayout areaWhereLayoutX = new BoxLayout(textAreaWhere, BoxLayout.X_AXIS);
        textAreaWhere.setFont(listPrompt.getFont());
        textAreaWhere.setLayout(areaWhereLayoutX);
        textAreaWhere.setMinimumSize(new Dimension(listWidth, 50));
        textAreaWhere.setPreferredSize(new Dimension(listWidth, 50));
        textAreaWhere.setMaximumSize(new Dimension(listWidth, 50));

        // User input for ORDER BY ordering
        order = titles.getString("Order");
        labelOrder.setText(order + refresh + ".");
        BoxLayout labelOrderLayoutX = new BoxLayout(labelOrder, BoxLayout.X_AXIS);
        labelOrder.setFont(listPrompt.getFont());
        labelOrder.setLayout(labelOrderLayoutX);
        labelOrder.setMinimumSize(new Dimension(listWidth, 30));
        labelOrder.setPreferredSize(new Dimension(listWidth, 30));
        labelOrder.setMaximumSize(new Dimension(listWidth, 30));
        labelOrder.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        labelOrder.setForeground(DIM_BLUE); // blue
        labelOrder.setAlignmentX(LEFT_ALIGNMENT);

        BoxLayout areaOrderLayoutX = new BoxLayout(textAreaOrder, BoxLayout.X_AXIS);
        textAreaOrder.setFont(listPrompt.getFont());
        textAreaOrder.setLayout(areaOrderLayoutX);
        textAreaOrder.setMinimumSize(new Dimension(listWidth, 20));
        textAreaOrder.setPreferredSize(new Dimension(listWidth, 20));
        textAreaOrder.setMaximumSize(new Dimension(listWidth, 20));

        // Statement panel
        BoxLayout areaStmtLayoutX = new BoxLayout(textAreaStmtPanel, BoxLayout.X_AXIS);
        textAreaStmtPanel.setLayout(areaStmtLayoutX);
        //      textAreaStmtPanel.setMinimumSize(new Dimension(listWidth, 100));
        textAreaStmtPanel.setPreferredSize(new Dimension(listWidth, 100));
        textAreaStmtPanel.setMaximumSize(new Dimension(listWidth, 100));
        textAreaStmtPanel.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        scrollPaneStmt.setBorder(null);

        scrollPaneStmt.setViewportView(textAreaStmt);

        // Message panel
        BoxLayout msgLayoutX = new BoxLayout(listMsgPanel, BoxLayout.X_AXIS);
        listMsgPanel.setLayout(msgLayoutX);
        listMsgPanel.setMinimumSize(new Dimension(listWidth, 20));
        listMsgPanel.setPreferredSize(new Dimension(listWidth, 20));
        listMsgPanel.setMaximumSize(new Dimension(listWidth, 20));

        // Button panel
        BoxLayout buttonLayoutX = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonLayoutX);
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(dspButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(refreshButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(columnsButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.setMinimumSize(new Dimension(listWidth, 60));
        buttonPanel.setPreferredSize(new Dimension(listWidth, 60));
        buttonPanel.setMaximumSize(new Dimension(listWidth, 60));

        // Global panel contains all other window objects
        globalPanel = new JPanel();

        // Create and register row selection model (for selecting a single row)
        // ---------------------------------------
        rowSelectionModel = jTable.getSelectionModel();
        jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Row selection model registration
        rowSelectionModel.addListSelectionListener(sl -> {
            rowIndexList = (ListSelectionModel) sl.getSource();
            rowIndex = rowIndexList.getLeadSelectionIndex();
            if (!rowIndexList.isSelectionEmpty()) {
                rowIndex = rowIndexList.getLeadSelectionIndex();
            } // No row was selected
            else {
                rowIndex = -1;
            }
        });

        // Set Exit button activity
        // ------------------------
        exitButton.addActionListener(a -> {
            String aliasStmtText;
            try {
                stmt = conn.createStatement();
                aliasStmtText = "drop alias " + file_member.toUpperCase();
                stmt.execute(aliasStmtText);
            } catch (Exception exc) {
                //exc.printStackTrace();
            }
            dispose();
        });

        // Set Display button activity
        // ---------------------------
        dspButton.addActionListener(a -> {
            addNewRecord = false;
            message.setText("");
            textAreaStmt.setText("");

            if (rowIndexList != null) { // row index not empty
                if (rowIndex >= 0) {
                    // Remove list window container
                    listContentPane.removeAll();
                    rowIndex = rowIndexList.getLeadSelectionIndex();
                    // Create panel with data fields and buttons
                    buildDataWindow();
                    // Display data scroll pane with focus (to enable page keys)
                    this.add(scrollPaneData);
                    scrollPaneData.requestFocus();
                    pack();
                    setVisible(true);
                } else {
                    message.setText(noRowUpd);
                    message.setForeground(DIM_RED); // red
                    listMsgPanel.add(message);
                    textAreaStmt.setText(stmtText);
                    textAreaStmtPanel.add(scrollPaneStmt);
                    setVisible(true);
                }
            } else {
                message.setText(noRowUpd);
                message.setForeground(DIM_RED); // red
                listMsgPanel.add(message);
                textAreaStmt.setText(stmtText);
                textAreaStmtPanel.add(scrollPaneStmt);
                setVisible(true);
            }
        });

        // Set Refresh button activity
        // ---------------------------
        refreshButton.addActionListener(a -> {
            refreshTableList();
            message.setForeground(DIM_RED); // Dim red
            listMsgPanel.add(message);
            textAreaStmt.setText(stmtText);
            textAreaStmtPanel.add(scrollPaneStmt);
            setVisible(true);
        });

        // Set Columns button activity
        // ---------------------------
        columnsButton.addActionListener(a -> {
            new U_ColumnsJList(normalColumnList, selectFileName);
            jTable.setRowSelectionInterval(rowIndex, rowIndex); // Save the same selection
            buildListWindow(this.condition, this.ordering);
        });

        // Finish the window building
        // --------------------------
        textAreaStmt.setText(stmtText);
        textAreaStmtPanel.add(scrollPaneStmt);

        listMsgPanel.add(message);
        listMsgPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        // Lay out components in the window in groups
        GroupLayout layout = new GroupLayout(globalPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(titlePanel)
                        .addComponent(scrollPaneList)
                        .addComponent(labelWhere)
                        .addComponent(textAreaWhere)
                        .addComponent(labelOrder)
                        .addComponent(textAreaOrder)
                        .addComponent(textAreaStmtPanel)
                        .addComponent(listMsgPanel)
                        .addComponent(buttonPanel)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createSequentialGroup()
                        .addComponent(titlePanel)
                        .addComponent(scrollPaneList)
                        .addComponent(labelWhere)
                        .addComponent(textAreaWhere)
                        .addComponent(labelOrder)
                        .addComponent(textAreaOrder)
                        .addComponent(textAreaStmtPanel)
                        .addComponent(listMsgPanel)
                        .addComponent(buttonPanel)));

        // Put the layout to the global panel
        globalPanel.setLayout(layout);

        // Put the global panel to a scroll pane
        JScrollPane scrollPaneTable = new JScrollPane(globalPanel);
        scrollPaneTable.setBorder(null);

        listContentPane = getContentPane(); // Window container
        listContentPane.removeAll(); // Important for resizing the window!

        // Put scroll pane to the window container
        listContentPane.add(scrollPaneTable);

        // Y = set window size for full contents, N = set fixed window size
        if (autoWindowSize.equals("Y")) {
            globalWidth = listWidth + 45;
            globalHeight = listHeight + 485;
            //pack();
        } else {
            globalWidth = windowWidth;
            globalHeight = windowHeight;
        }

        // Select the row previously selected or a nearest lower.
        if (numOfRows > 0) {
            if (rowIndex < numOfRows) {
                jTable.setRowSelectionInterval(rowIndex, rowIndex);
            } else {
                jTable.setRowSelectionInterval(numOfRows - 1, numOfRows - 1);
            }
        }
        // The selected row will be shown as the first row in the list window
        jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(rowIndex, 0, true)));        

        // Make window visible
        setSize(globalWidth, globalHeight);
        setLocation(0, 10);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * ************************************************************************
     * Builds data Window for displaying data
     * *************************************************************************
     */
    Integer dataPanelGlobalWidth = 640;
    Integer minFldWidth = 40;
    JPanel dataGlobalPanel;
    //Container dataContentPane;
    JPanel dataMsgPanel = new JPanel();
    JScrollPane scrollPaneData;

    JTextField[] textFields;

    JLabel[] fldLabels;
    String[] txtFldLengths;

    GridBagLayout gridBagLayout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    // Multiplication factor for the data field width (in characters)
    // to get field width in pixels
    int dataFieldFactor;

    /**
     * Build the window with data to be displaye.
     */
    String colName;
    JTextArea resultTextArea;
    int insertColumnNumber;
    PreparedStatement pstmt;
    ArrayList<String> values;
    long colStartPos;
    long colLength;
    Reader reader;
    InputStream stream;



    protected void buildDataWindow() {

        colStartPos = 1;
        colLength = parseInt(fetchFirst);

        // Start building the window
        // -------------------------
        JLabel dataPanelTitle = new JLabel();
        enterData = titles.getString("EnterData");
        String panelText = enterData + library.toUpperCase() + "/" + db_file.toUpperCase();

        // If file name differs from member name - add member name in parentheses
        if (!db_file.equalsIgnoreCase(file_member)) {
            panelText += "(" + file_member + ")";
        }
        dataPanelTitle.setText(panelText);
        dataPanelTitle.setFont(new Font("Helvetica", Font.PLAIN, 20));

        JPanel titleDataPanel = new JPanel();
        titleDataPanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        titleDataPanel.add(dataPanelTitle);
        titleDataPanel.setMinimumSize(new Dimension(dataPanelTitle.getPreferredSize().width, 30));
        titleDataPanel.setPreferredSize(new Dimension(dataPanelTitle.getPreferredSize().width, 30));
        titleDataPanel.setMaximumSize(new Dimension(dataPanelTitle.getPreferredSize().width, 30));

        // Create arrays of labels and (empty) text fields
        fldLabels = new JLabel[numOfCols];
        textFields = new JTextField[numOfCols];
        for (int idx = 0; idx < numOfCols; idx++) {
            // Column name, type and size
            if (colTypes[idx].equals("NUMERIC") || colTypes[idx].equals("DECIMAL")) {
                fldLabels[idx] = new JLabel(colNames[idx] + "  " + colTypes[idx] + " (" + colPrecisions[idx] + ", " + colScales[idx] + ")");
            } else {
                fldLabels[idx] = new JLabel(colNames[idx] + "  " + colTypes[idx] + " (" + colSizes[idx] + ")");
            }
            // Empty text field
            textFields[idx] = new JTextField("");
        }

        // Place data fields in grid bag for all columns
        // to input data panel
        // ---------------------------------------------
        JPanel inputDataPanel = new JPanel();
        // Grid bag layout used to lay out components
        inputDataPanel.setLayout(gridBagLayout);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridy = 0;

        // Create input text fields of normal table columns
        // ------------------------------------------------
        // RRN field (index 0) is omitted!
        dataFieldFactor = (int) (fontSize * 0.9);

        // Data in fields have default font and its size
        //Font defaultFont = UIManager.getDefaults().getFont("TabbedPane.font");
        // Text fields are proportional to the font size
        //dataFieldFactor = (int) (defaultFont.getSize());
        for (int idx = 1; idx < numOfCols; idx++) {
            int txtFieldLength = colSizes[idx] * dataFieldFactor;
            // Binary and variable binary columns will have twice as many characters (hex)
            if (colTypes[idx].equals("BINARY") || colTypes[idx].equals("VARBINARY")) {
                txtFieldLength *= 2;
            }
            if (txtFieldLength > maxFldWidth) {
                txtFieldLength = maxFldWidth;
            }
            if (txtFieldLength < minFldWidth) {
                txtFieldLength = minFldWidth;
            }
            textFields[idx].setMinimumSize(new Dimension(txtFieldLength, 25));
            textFields[idx].setMaximumSize(new Dimension(txtFieldLength, 25));
            textFields[idx].setPreferredSize(new Dimension(txtFieldLength, 25));
            textFields[idx].setFont(new Font("Monospaced", Font.PLAIN, fontSize));
            // textFields[idx].setFont(new Font("Monospaced", Font.PLAIN, defaultFont.getSize()));

            if (rowIndex > -1) { // -1 was set in "Display Selected" if no row was selected
                if (rows[rowIndex][idx] == null) {
                    textFields[idx].setText(nullMark);
                } else {
                    textFields[idx].setText((rows[rowIndex][idx]).toString());
                }
            }

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;

            // Add column labels to the input data panel on the left
            inputDataPanel.add(fldLabels[idx], gbc);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;

            // Add text fields to the input data panel on the right
            inputDataPanel.add(textFields[idx], gbc);
        }

        JButton dataPanelReturnButton = new JButton(_return);
        dataPanelReturnButton.setMinimumSize(new Dimension(80, 35));
        dataPanelReturnButton.setMaximumSize(new Dimension(80, 35));
        dataPanelReturnButton.setPreferredSize(new Dimension(80, 35));

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
        buttonRow.setAlignmentX(Box.LEFT_ALIGNMENT);
        buttonRow.add(dataPanelReturnButton);

        // Message panels
        BoxLayout msgLayoutX = new BoxLayout(dataMsgPanel, BoxLayout.LINE_AXIS);
        dataMsgPanel.setAlignmentX(Box.LEFT_ALIGNMENT);
        dataMsgPanel.setLayout(msgLayoutX);

        dataMsgPanel.removeAll();

        // Lay out components in groups
        dataGlobalPanel = new JPanel();
        GroupLayout layout = new GroupLayout(dataGlobalPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(titleDataPanel)
                .addComponent(buttonRow)
                .addComponent(dataMsgPanel)
                .addComponent(inputDataPanel)
        ));
        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createSequentialGroup()
                .addComponent(titleDataPanel)
                .addComponent(buttonRow)
                .addComponent(dataMsgPanel)
                .addComponent(inputDataPanel)
        ));

        dataGlobalPanel.setLayout(layout);

        // Put data global panel to the scroll pane
        scrollPaneData = new JScrollPane(dataGlobalPanel);

        // Set Return button activity
        // --------------------------
        dataPanelReturnButton.addActionListener(a -> {
            listContentPane.removeAll();
            jTable.setRowSelectionInterval(rowIndex, rowIndex); // Save the same selection
            buildListWindow(this.condition, this.ordering);
        });

        // Enable ENTER key to save and return action
        // ------------------------------------------
        dataGlobalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "saveData");
    }

    /**
     * Evaluate modifications of the SELECT statement: WHERE condition and ORDER BY ordering.
     */
    protected void evalModifications() {
        condition = textAreaWhere.getText();
        if (!condition.equals("")) {
            whereClause = " WHERE " + condition;
        } else {
            textAreaWhere.setText("");
            whereClause = "";
        }
        ordering = textAreaOrder.getText();
        if (!ordering.equals("")) {
            orderByClause = " ORDER BY " + ordering;
        } else {
            textAreaOrder.setText("");
            orderByClause = "";
        }
    }

    /**
     * Refresh the list of rows in the table according to WHERE and ORDER BY modifications (if applicable).
     */
    protected void refreshTableList() {
        message.setText("");

        condition = textAreaWhere.getText();
        ordering = textAreaOrder.getText();

        // Save the modifications of the SELECT statement
        // obtained from the input text areas to the file.
        ArrayList<String> modifArr = new ArrayList<>();
        // Build an array of 2 items - condition and ordering
        // divided by a semicolon. Both items may contain New Line characters.
        modifArr.add(condition + ";");
        modifArr.add(ordering);
        // Write the array to the file thus preserving the user input
        try {
            // Write file with values of condition (WHERE) and ordering (ORDER BY)
            // Rewrite the existing file or create and write a new file.
            Files.write(selectPath, modifArr, StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            System.out.println("write file: " + ioe.getLocalizedMessage());
            ioe.printStackTrace();
        }

        // Re-create the table
        listContentPane.removeAll();

        buildListWindow(condition, ordering);
    }

    /**
     * Get data from the database file (table)
     *
     * @return
     */
    public JLabel getData() {
        String columnList;
        String[] columnArray;
        // Read actual select column list from the .col file
        try {
            List<String> items = Files.readAllLines(columnsPath);
            // System.out.println("items.toString(): "+items.toString());
            columnList = items.get(0);
            columnArray = columnList.split(",");
            actualColumnList = "";
            for (int idx = 1; idx < columnArray.length; idx++) {
                // 20 columns in a line, next columns in the next line
                if (idx % 20 != 0) {
                    actualColumnList += "," + columnArray[idx];
                } else {
                    actualColumnList += "\n";
                    actualColumnList += "," + columnArray[idx];
                }
            }
            // System.out.println("actualColumnList:"+actualColumnList);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        ResultSet rs;
        try {
            // Build text of SELECT statement using column names and other variables.
            // ------------------------------
            // The first column will allways be the value of the RRN (Relative Record Number) 
            // of the given row in the original table.

            // Example:
            // select rrn(CENY2) as RRN, CZBOZI, CENAJ, NAZZBO, RAZITKO, DATUM, CAS
            //   from VZTOOL/CENY2
            //   fetch first 1000 rows only
            stmtText = "select rrn(" + file_member.toUpperCase() + ") as RRN" + actualColumnList
                    + "\n from " + library.toUpperCase() + "/" + file_member.toUpperCase();
            if (!whereClause.isEmpty()) {
                stmtText += "\n ";
            }
            stmtText += whereClause;
            if (!orderByClause.isEmpty()) {
                stmtText += "\n ";
            }
            stmtText += orderByClause;
            stmtText += "\n fetch first " + fetchFirst + " rows only";

            // Create statement Scroll insensitive and Concurrent updatable
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            //            stmt = conn.createStatement();

            // Execute the SQL statement and obtain the Result Set rs.
            // -------------------------
            rs = stmt.executeQuery(stmtText);
            // An error may result from invalid statement

            // Get information on columns of the result set
            ResultSetMetaData rsmd = rs.getMetaData(); // data about result set
            numOfCols = rsmd.getColumnCount(); // number of columns in result set
            colNames = new String[numOfCols];
            colSizes = new int[numOfCols];
            colPrecisions = new String[numOfCols];
            colScales = new String[numOfCols];
            colTypes = new String[numOfCols];
            // Omit column 0 with RRN???
            listWidth = 0;
            for (int col = 0; col < numOfCols; col++) {
                colNames[col] = rsmd.getColumnName(col + 1);
                colTypes[col] = rsmd.getColumnTypeName(col + 1);
                colSizes[col] = rsmd.getColumnDisplaySize(col + 1);
                int colSize = colSizes[col];
                if (colTypes[col].equals("BINARY") || colTypes[col].equals("VARBINARY")) {
                    colSize *= 2;
                } else if (colTypes[col].equals("NUMERIC") || colTypes[col].equals("DECIMAL")) {
                    //System.out.println("Precision: " + rs.getMetaData().getPrecision(col + 1));
                    //System.out.println("Scale    : " + rsmd.getScale(col + 1));
                    colPrecisions[col] = String.valueOf(rs.getMetaData().getPrecision(col + 1));
                    colScales[col] = String.valueOf(rs.getMetaData().getScale(col + 1));
                }
                double maxFieldWidth = cellFieldFactor * Math.max(colSize, colNames[col].length());
                if (maxFieldWidth > maxFldWidth) {
                    maxFieldWidth = maxFldWidth;
                }
                // Add column widths to determine width of the window
                listWidth += maxFieldWidth;
            }
            // Reduce window width by part of RRN column width (BigDecimal)
            listWidth -= 100;
            // The width must fit all objects (mainly buttons)
            if (listWidth < 850) {
                listWidth = 850;
            }

            // Fill "rows" array by values from the result set rs
            // --------------------------------------------------
            // Set end of result set
            rs.last();
            // Get number of rows (the number of the last row)
            numOfRows = rs.getRow();
            rs.beforeFirst(); // set pointer before the first row

            // Create the "rows" array now when number of elements is known
            rows = new Object[numOfRows][numOfCols];
            while (rs.next()) {
                // Fields are numbered from 0, database columns from 1
                for (int col = 0; col < numOfCols; col++) {
                    // System.out.println("colTypes[col]: " + colTypes[col]);
                    // System.out.println("rs.getObject(col + 1): " +
                    // rs.getObject(col + 1));
                    // Set cell value
                    if (colTypes[col] == null) {
                        // If column value is null set null mark to the cell                        
                        rows[rs.getRow() - 1][col] = nullMark;
                    } else if (colTypes[col].equals("BINARY") || colTypes[col].equals("VARBINARY")) {
                        // If column type is BINARY or VARBINARY translate bytes in
                        // hexadecimal characters
                        int length = rs.getBytes(col + 1).length;
                        String hexString = "";
                        for (int idx = 0; idx < length; idx++) {
                            hexString += byteToHex(rs.getBytes(col + 1)[idx]);
                            // System.out.println("hexString: "+hexString);
                        }
                        rows[rs.getRow() - 1][col] = hexString;
                    } else {
                        // Otherwise set the cell value as Object
                        // that is automatically converted to the correct type.                        
                        rows[rs.getRow() - 1][col] = rs.getObject(col + 1);
                    }
                }
            }
            rs.close();
            stmt.close();
        } // end try
        catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("Statement:\n" + stmtText);
            System.out.println("getData: " + exc.toString());
            // Connection to the server lost or data error
            message.setText(connLost + " - " + exc.toString() + "\n");
            // Initialize the .sel file that has invalid data (WHERE/ORDER BY).
            // If invalid data remained in the file after exiting the window
            // the window would never been displayed again.
            try {
                selectPath = Paths.get(System.getProperty("user.dir"), "selectfiles", selectFileName + ".sel");
                ArrayList<String> lines = new ArrayList<>();
                // The file has initially exactly two lines.
                // The first line contains a semicolon, the second line is empty.
                lines.add(";");
                lines.add("");
                // Create the file from the array list "lines" with two empty lines
                Files.write(selectPath, lines, Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Display only 2 columns in 1 row
            // - the first is RRN, the second is an explaining text
            numOfCols = 2; // one column only
            numOfRows = 0; // no rows in table
            colNames = new String[2];
            colNames[0] = "!";
            colNames[1] = noData;
            colSizes = new int[2];
            colSizes[0] = 1;
            colSizes[1] = 1200; // Get enough space for long messages
            // Create a new data array
            rows = new Object[numOfRows][numOfCols];
            // System.out.println("colNames[1]: "+colNames[1]);
            //            listWidth = 1200;
            message.setForeground(DIM_RED); // red local message
        }
        return message;
    }

    /**
     * Create jTable in scrollPane in listPanel
     */
    protected void createTable() {
        // Create a new table with its own data model
        // ------------------------------------------
        tableModel = new TableModel();
        jTable = new JTable(tableModel);

        // Attributes of the table
        jTable.setRowHeight(25); // row height
        // color of grid lines
        jTable.setGridColor(Color.LIGHT_GRAY);
        // no resizing of columns 
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // font in cells
        jTable.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        // font in header
        jTable.getTableHeader().setFont(new Font("Monospaced", Font.ITALIC, fontSize));
        // header height
        jTable.getTableHeader().setPreferredSize(new Dimension(0, 26));
        // no reordering of headers and columns
        jTable.getTableHeader().setReorderingAllowed(false);

        // Column model for column rendering and editing
        TableColumnModel tcm = jTable.getColumnModel();
        tc = new TableColumn[numOfCols];
        for (int col = 0; col < numOfCols; col++) {
            // Get table column object from the model
            tc[col] = tcm.getColumn(col);
            // Assign the cell editor to the table column
        }
        // Column 0 - RRN - different background and foreground color
        tc[0].setCellRenderer(new ColorColumnRenderer(Color.WHITE, DIM_BLUE));
        tc[0].setHeaderRenderer(new ColorColumnRenderer(Color.WHITE, DIM_BLUE));

        // Column 0 - RRN - width will be adjusted to fit the column NAME.
        tc[0].setPreferredWidth((int) (Math.max(colSizes[0], cellFieldFactor * colNames[0].length()) + 5));

        // Other columns width will be adjusted by multiplication with cellFieldFactor
        for (int col = 1; col < numOfCols; col++) {
            int colSize = colSizes[col];
            if (colTypes != null) {
                // If the member of the file exists - columns may be processed
                if (colTypes[col].equals("BINARY") || colTypes[col].equals("VARBINARY")) {
                    // Binary columns are twice as wide
                    colSize *= 2;
                }

                double maxFieldWidth = cellFieldFactor * Math.max(colSize, colNames[col].length());
                if (maxFieldWidth > maxFldWidth) {
                    maxFieldWidth = maxFldWidth;
                }
                tc[col].setPreferredWidth((int) maxFieldWidth);
                //            tc[col].setMaxWidth((int) maxFieldWidth);
                //            tc[col].setMinWidth((int) maxFieldWidth);
            } else {
                // If no member exists in the file the error message must be visible in the window
                tc[col].setPreferredWidth((int) 200);
                listWidth = 1200;
            }
        }

        // Fixed height of the table within the window
        listHeight = 400;

        scrollPaneList = new JScrollPane(jTable);
        scrollPaneList.setMaximumSize(new Dimension(listWidth, listHeight));
        scrollPaneList.setMinimumSize(new Dimension(listWidth, listHeight));
        scrollPaneList.setPreferredSize(new Dimension(listWidth, listHeight));
        scrollPaneList.setBorder(BorderFactory.createLineBorder(Color.WHITE));
    }

//    JLabel msg = new JLabel();

    /**
     * Data model provides methods to fill data from the source to table cells
     * for display. It is applied every time when any change in data source
     * occurs.
     */
    class TableModel extends AbstractTableModel {

        static final long serialVersionUID = 1L;

        // Returns number of columns
        @Override
        public int getColumnCount() {
            return numOfCols;
        }

        // Returns number of rows
        @Override
        public int getRowCount() {
            return numOfRows;
        }

        // Sets number of rows
        public void setRowCount(int rowCount) {
            numOfRows = rowCount;
        }

        // Returns column name
        @Override
        public String getColumnName(int col) {
            return colNames[col];
        }

        // Data transfer from the source to a cell for display. It is applied
        // automatically at any change of data source but also when ENTER or TAB key
        // is pressed or when clicked by a mouse. Double click or pressing a data key
        // invokes the cell editor method - getTableCellEditorComponent().
        // The method is called at least as many times as is the number of cells
        // in the table.
        @Override
        public Object getValueAt(int row, int col) {
            // System.out.println("getValueAt: (" + row + "," + col + "): " +
            // rows[row][col]);
            // Return the value for display in the table
            if (rows[row][col] == null) {
                return nullMark; // Sloupec s hodnotou NULL
            } else {
                return rows[row][col].toString(); // Ostatní sloupce
            }
        }

        // Write input data from the cell back to the data source for
        // display in the table. A change in the data source invokes method
        // getValueAt().
        // The method is called after the cell editor ends its activity.
        @Override
        public void setValueAt(Object obj, int row, int col) {
            // Assign the value from the cell to the data source.
            if (obj == null) {
                rows[row][col] = null;
            }
            rows[row][col] = obj;
            // System.out.println("setValueAt: (" + row + "," + col + "): " + rows[row][col]);
        }

        // Get class of the column value - it is important for the cell editor
        // could be invoked and could determine e.g. the way of aligning of the
        // text in the cell.
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Class getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        // Determine whicn cells are editable or not
        @Override
        public boolean isCellEditable(int row, int col) {
            return false; // column 0 - RRN - cannot be changed
        }
    }

    /**
     * Determine background and foreground color in the column
     */
    class ColorColumnRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        Color backgroundColor, foregroundColor;

        public ColorColumnRenderer(Color backgroundColor, Color foregroundColor) {
            super();
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setBackground(backgroundColor);
            cell.setForeground(foregroundColor);
            return cell;
        }
    }

    /**
     * Determint alignment of data in the column according to the data type.
     * BigDecimal and Integer are aligned to the right, the others are aligned
     * left.
     */
    class AdjustColumnRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        String cls;

        public AdjustColumnRenderer(String cls) {
            this.cls = cls;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (cls.equals("java.math.BigDecimal") || cls.equals("java.lang.Integer")) {
                renderedLabel.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
            }
            return renderedLabel;
        }
    }

    /**
     * Translate single byte to hexadecimal character
     *
     * @param singleByte
     * @return
     */
    static String byteToHex(byte singleByte) {
        int bin = (singleByte < 0) ? (256 + singleByte) : singleByte;
        int bin0 = bin >>> 4; // higher half-byte
        int bin1 = bin % 16; // lowe half-byte
        String hex = Integer.toHexString(bin0) + Integer.toHexString(bin1);
        return hex;
    }

    /**
     * Translate a string of two hexadecimal characters to a single byte
     *
     * @param hexChar
     * @return
     */
    static byte hexToByte(String hexChar) {
        // Translation tables
        String args = "0123456789abcdef";
        int[] funcs = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        // Two characters from the String traslated in lower case
        char charHigh = hexChar.toLowerCase().charAt(0);
        char charLow = hexChar.toLowerCase().charAt(1);
        int highHalf = 0, lowHalf = 0;
        // Find character in argument table
        if (args.indexOf(charHigh) > -1) // If found get corresponding function (int value)
        // If not found - result is 0
        {
            highHalf = funcs[args.indexOf(charHigh)];
        }
        if (args.indexOf(charLow) > -1) {
            lowHalf = funcs[args.indexOf(charLow)];
        }
        // Assemble high and low half-bytes in single byte
        int singleByte = (highHalf << 4) + lowHalf;
        return (byte) singleByte;
    }
}
