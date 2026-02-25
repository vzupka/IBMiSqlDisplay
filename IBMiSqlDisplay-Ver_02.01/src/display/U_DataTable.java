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
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Blob;
import java.sql.Clob;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JLabel;
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
    final String PROP_COMMENT = "Sql Display for IBM i, © Vladimír Župka 2015, 2025";

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;

    String library;
    String fileName;
    String file_member;
    String aliasStmtText;
    String language;
    String nullMark;
    int fontSize;
    int windowHeight;
    int windowWidth;
    String autoWindowSize;
    String fetchFirst;

    int maxFldWidth;

    // Connection to database
    Connection connection;

    // CLOB object
    Clob clob;
    U_ClobReturnedValues retClobValues;

    // BLOB object
    Blob blob;
    Blob blobReturned;
    long blobLength;
    U_BlobReturnedValues retBlobValues;

    // SQL attributes
    Statement stmt; // SQL statement object
    String stmtText; // SELECT statement text
    String condition; // condition in WHERE clause
    String whereClause; // WHERE clause (WHERE + condition)
    String ordering; // value in ORDER BY clause
    String orderByClause; // ORDER BY clause (ORDER BY + ordering)
    String actualColumnList = ""; // actual list of column names for SELECT
    String normalColumnList = ""; // normal list of column names for SELECT
    String blobColumnList = ""; // list of BLOB type columns

    ArrayList<String> allColNames; // all column names
    ArrayList<String> allColTypes; // all column types
    ArrayList<Integer> allColSizes; // all max. sizes of columns
    ArrayList<String> clobColNames; // clob column names
    ArrayList<String> clobColTypes; // clob column types
    ArrayList<Integer> clobColSizes; // clob max. sizes of columns

    ArrayList<String> blobColNames; // blob column names
    ArrayList<String> blobColTypes; // blob column types
    ArrayList<Integer> blobColSizes; // blob max. sizes of columns

    String[] colNames; // result set column names
    String[] colTypes; // result set column types
    int[] colSizes; // result set max. sizes of columns (number of characters)
    String[] colPrecisions; // number of digits
    String[] colScales; // number of decimal positions
    int colCapacity; // column capacity = size of CLOB or BLOB

    // Graphical table attributes
    JTable jTable; // graphical table
    TableModel tableModel; // JTable data model
    TableColumn[] tc; // table column array for rendering rows
    public Object[][] rows; // rows of JTable (two-dimensional array)
    int numOfRows; // number of rows in result set
    int numOfCols; // number of columns in result set
    int allNumOfCols; // number of all columns (including CLOB, ...)
    int clobNumOfCols; // number of CLOB columns
    int blobNumOfCols; // number of BLOB columns

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
    JButton showButton;
    JButton refreshButton;
    JButton columnsButton;

    // Containers for building the list
    JPanel titlePanel;
    JScrollPane scrollPaneList;
    JScrollPane scrollPaneStmt;
    JPanel buttonPanel;
    JPanel listMsgPanel;
    JPanel globalPanel;
    Container listContentPane;
    int listWidth, listHeight;
    int globalWidth, globalHeight;

    // Localized text objects
    ResourceBundle titles;
    String titEdit, changeCell, where, order, dataTab;
    ResourceBundle buttons;
    String exit, edit_sel, refresh, columns, _return;
    ResourceBundle locMessages;
    String noRowUpd, noData, noDataMember, sqlError, invalidValue, txtLength,
            tooLongForCol,colValueNull, contentNotLoaded;

    File file = null;

    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = Color.getHSBColor(.003f, .90f, .40f);
    final Color VERY_LIGHT_BLUE = Color.getHSBColor(0.60f, 0.05f, 0.98f);

    /**
     * Constructor
     *
     * @param connection
     */
    @SuppressWarnings("ConvertToTryWithResources")

    public U_DataTable(Connection connection) {
        this.connection = connection;
        try {
            // Read file with application properties from directory "paramfiles"
            properties = new Properties();
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();

            // Get and adjust properties to variables
            library = properties.getProperty("LIBRARY"); // library name
            fileName = properties.getProperty("FILE"); // file name
            file_member = properties.getProperty("MEMBER"); // member name
            language = properties.getProperty("LANGUAGE"); // local language
            windowHeight = Integer.parseInt(properties.getProperty("RESULT_WINDOW_HEIGHT"));
            windowWidth = Integer.parseInt(properties.getProperty("RESULT_WINDOW_WIDTH"));
            autoWindowSize = properties.getProperty("AUTO_WINDOW_SIZE");
            nullMark = properties.getProperty("NULL_MARK");
            fontSize = Integer.parseInt(properties.getProperty("FONT_SIZE"));
            // Factor to multiply cell width
            cellFieldFactor = fontSize * 0.75;
            // Max. number of rows in the result set
            fetchFirst = properties.getProperty("FETCH_FIRST");
            maxFldWidth = Integer.parseInt(properties.getProperty("MAX_FIELD_LENGTH"));

            // If the member name is *FIRST, change it to the file name
            if (file_member.toUpperCase().equals("*FIRST")) {
                file_member = fileName;
                // but save *FIRST to properties
                properties.setProperty("MEMBER", "*FIRST");
            } else {
                // Save user entered member name to properties
                properties.setProperty("MEMBER", file_member.toUpperCase());
                try {
                    stmt = connection.createStatement();
                    aliasStmtText = "DROP ALIAS " + file_member.toUpperCase();
                    stmt.execute(aliasStmtText);
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
                try {
                    aliasStmtText = "CREATE ALIAS " + file_member.toUpperCase() + 
                            " for " + fileName.toUpperCase() + "(" + file_member + ")";
                    stmt.execute(aliasStmtText);
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
            }
            
            // Create a new text file in directory "paramfiles" and store properties there
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
        selectFileName = library.toUpperCase() + "-" + fileName.toUpperCase();
        selectPath = Paths.get(System.getProperty("user.dir"), "selectfiles", selectFileName + ".sel");

        // Read the file and get values of condition (WHERE)
        // and ordering (ORDER BY)
        try {
            // Create the file with two lines (if the file does not exist)
            if (!Files.exists(selectPath)) {
                ArrayList<String> lines = new ArrayList<>();
                // The file has initially exactly two lines.
                // The first line contains a semicolon,
                // The second line is empty.
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
        } catch (IOException exc) {
            exc.printStackTrace();
            // message.setText(colNotText);
            message.setText(exc.toString());
        }

        // Get full column list from the metadata of the table
        normalColumnList = "";
        blobColumnList = "";
        stmtText = """
                   SELECT COLUMN_NAME, DATA_TYPE, CCSID,
                           LENGTH,  NUMERIC_SCALE 
                     FROM QSYS2.SYSCOLUMNS
                     WHERE TABLE_NAME = ? AND
                     TABLE_SCHEMA = ?""" ;
        //System.out.println("stmtText: " + stmtText);
        try {
            pstmt = connection.prepareStatement(stmtText);
            pstmt.setObject(1, fileName);
            pstmt.setObject(2, library);
            pstmt.executeQuery();
            ResultSet rs = pstmt.getResultSet();

            allNumOfCols = 0;
            allColNames = new ArrayList<>();
            allColTypes = new ArrayList<>();
            allColSizes = new ArrayList<>();
            clobNumOfCols = 0;
            clobColNames = new ArrayList<>();
            clobColTypes = new ArrayList<>();
            clobColSizes = new ArrayList<>();
            blobNumOfCols = 0;
            blobColNames = new ArrayList<>();
            blobColTypes = new ArrayList<>();
            blobColSizes = new ArrayList<>();

            while (rs.next()) {
                colName = rs.getString(1);
                String colType = rs.getString(2);
                int colSize = rs.getInt(4);
                allColNames.add(colName);
                allColTypes.add(colType);
                allColSizes.add(colSize);

                allNumOfCols++;
                // Omit advanced column types
                // - they are incapable to render in a cell
                if (!"CLOB".equals(colType) && !"BLOB".equals(colType)
                        && !"ARRAY".equals(colType)) // Add column name to the full list without advanced column types
                // - they are incapable to render in a cell
                {
                    normalColumnList += ", " + colName;
                }

                // Advanced columns are added to special lists
                if ("CLOB".equals(colType)) {
                    clobNumOfCols++;
                    clobColNames.add(colName);
                    clobColTypes.add(colType);
                    clobColSizes.add(colSize);
                }

                if ("BLOB".equals(colType)) {
                    blobNumOfCols++;
                    blobColumnList += ", " + colName;
                    blobColNames.add(colName);
                    blobColTypes.add(colType);
                    blobColSizes.add(colSize);
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
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

        showButton = new JButton(edit_sel);
        showButton.setMinimumSize(new Dimension(130, 35));
        showButton.setMaximumSize(new Dimension(130, 35));
        showButton.setPreferredSize(new Dimension(130, 35));

        refreshButton = new JButton(refresh);
        refreshButton.setMinimumSize(new Dimension(140, 35));
        refreshButton.setMaximumSize(new Dimension(140, 35));
        refreshButton.setPreferredSize(new Dimension(140, 35));

        columnsButton = new JButton(columns);
        columnsButton.setMinimumSize(new Dimension(140, 35));
        columnsButton.setMaximumSize(new Dimension(140, 35));
        columnsButton.setPreferredSize(new Dimension(140, 35));

        titlePanel = new JPanel();
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
        noDataMember = locMessages.getString("NoDataMember");
        sqlError = locMessages.getString("SqlError");
        invalidValue = locMessages.getString("InvalidValue");
        colValueNull = locMessages.getString("ColValueNull");

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
        BoxLayout boxLayoutY = new BoxLayout(titlePanel, BoxLayout.Y_AXIS);
        titlePanel.setLayout(boxLayoutY);
        titlePanel.setAlignmentX(Box.LEFT_ALIGNMENT);

        // Localized titles
        titEdit = titles.getString("TitEdit") + library.toUpperCase() + "/" + fileName.toUpperCase();

        // If file name differs from member name - add member name in parentheses
        if (!fileName.equalsIgnoreCase(file_member)) {
            titEdit += "(" + file_member.toUpperCase() + ")";
        }
        listTitle.setText(titEdit);
        listTitle.setFont(new Font("Helvetica", Font.PLAIN, 20));
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
        textAreaStmtPanel.setPreferredSize(new Dimension(listWidth, 100));
        textAreaStmtPanel.setMaximumSize(new Dimension(listWidth, 100));
        textAreaStmtPanel.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        scrollPaneStmt.setBorder(null);

        scrollPaneStmt.setViewportView(textAreaStmt);

        // Message panel
        BoxLayout msgLayoutX = new BoxLayout(listMsgPanel, BoxLayout.X_AXIS);
        listMsgPanel.setLayout(msgLayoutX);

        // Button panel
        BoxLayout buttonLayoutX = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonLayoutX);
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(showButton);
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
            // Drop alias for the member if the name differs from file name
            if (!fileName.equalsIgnoreCase(file_member)) {
                try {
                    stmt = connection.createStatement();
                    aliasStmtText = "DROP ALIAS " + file_member.toUpperCase();
                    stmt.execute(aliasStmtText);
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
            }
            dispose();  // close the data window and return to the list window
        });

        // Set Show button activity
        // --------------------------
        showButton.addActionListener(a -> {
            addNewRecord = false;
            message.setText("");
            textAreaStmt.setText("");

            if (rowIndexList != null) { // Row index is not empty
                if (rowIndex >= 0) {
                    // Remove list window container
                    listContentPane.removeAll();
                    rowIndex = rowIndexList.getLeadSelectionIndex();
                    // Create panel with data fields and buttons
                    buildDataWindow();
                    // Display data scroll pane with focus (to enable page keys)
                    this.add(dataGlobalPanel);
                    scrollPaneData.requestFocus();
                    pack();
                    setVisible(true);
                    rowIndexList = null;
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
        globalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // Put the global panel to a scroll pane
        JScrollPane scrollPaneTable = new JScrollPane(globalPanel);
        scrollPaneTable.setBorder(null);

        listContentPane = getContentPane(); // Window container
        listContentPane.removeAll(); // Important for resizing the window!

        // Put scroll pane to the window container
        listContentPane.add(scrollPaneTable);

        // Y = set window size for full contents, N = set fixed window size
        if (autoWindowSize.equals("Y")) {
            globalWidth = listWidth + 22;
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
     * Builds data Window for inserting or updating data
     * *************************************************************************
     */
    Integer minFldWidth = 40;
    JPanel dataGlobalPanel;
    //Container dataContentPane;
    JPanel dataMsgPanel = new JPanel();
    JScrollPane scrollPaneData;

    JLabel[] textLabels;
    JLabel[] fldLabels;

    ArrayList<JLabel> clobLabels;
    ArrayList<JButton> clobButtons;
    ArrayList<JLabel> blobLabels;
    ArrayList<JButton> blobButtons;

    String clobTypes[];

    GridBagLayout gridBagLayout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    // Multiplication factor for the data field width (in characters)
    // to get field width in pixels
    int dataFieldFactor;

    /**
     * Build the window with data to be entered or rewritten.
     */
    String colName;
    PreparedStatement pstmt;
    long colStartPos;
    long colLength;
    Reader reader;
    InputStream stream;

    protected void buildDataWindow() {

        colStartPos = 1;
        colLength = Integer.parseInt(fetchFirst);

        // Localized messages
        txtLength = locMessages.getString("Length");
        tooLongForCol = locMessages.getString("TooLongForCol");
        contentNotLoaded = locMessages.getString("ContentNotLoaded");

        // Start building the window
        // -------------------------
        JLabel dataPanelTitle = new JLabel();
        dataTab = titles.getString("DataTab");
        String panelText = dataTab + library.toUpperCase() + "/" + fileName.toUpperCase();

        // If file name differs from member name - add member name in parentheses
        if (!fileName.equalsIgnoreCase(file_member)) {
            panelText += "(" + file_member.toUpperCase() + ")";
        }
        dataPanelTitle.setText(panelText);
        dataPanelTitle.setFont(new Font("Helvetica", Font.PLAIN, 20));

        JPanel titleDataPanel = new JPanel();
        titleDataPanel.setLayout(new BoxLayout(titleDataPanel, BoxLayout.X_AXIS));
        titleDataPanel.add(dataPanelTitle);
        titleDataPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create arrays of labels and (empty) text fields
        fldLabels = new JLabel[numOfCols];
        textLabels = new JLabel[numOfCols];
        for (int idx = 0; idx < numOfCols; idx++) {
            // Column name, type and size
            switch (colTypes[idx]) {
                case "NUMERIC":
                case "DECIMAL":
                    fldLabels[idx] = new JLabel(colNames[idx] + "   " + colTypes[idx] +
                            " (" + colPrecisions[idx] + ", " + colScales[idx] + ")");
                    break;
                case "CHAR":
                case "VARCHAR":
                    fldLabels[idx] = new JLabel(colNames[idx] + "   " + colTypes[idx] + " (" + colSizes[idx] + ")");
                    break;
                default:
                    fldLabels[idx] = new JLabel(colNames[idx] + "   "
                            + "" + colTypes[idx] + " (" + colSizes[idx] + ")");
                    break;
            }
            // Empty text field
            textLabels[idx] = new JLabel("");
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
            //textLabels[idx].setMinimumSize(new Dimension(txtFieldLength, 25));
            //textLabels[idx].setMaximumSize(new Dimension(txtFieldLength, 25));
            //textLabels[idx].setPreferredSize(new Dimension(txtFieldLength, 25));
            textLabels[idx].setFont(new Font("Monospaced", Font.PLAIN, fontSize));
            textLabels[idx].setForeground(DIM_RED);

            if (rows.length > 0) {  // if the file is not empty (has at least one row)
                if (rows[rowIndex][idx] == null) {
                    textLabels[idx].setText(nullMark);
                } else {
                    textLabels[idx].setText((rows[rowIndex][idx]).toString());
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
            inputDataPanel.add(textLabels[idx], gbc);
        }

        // Create labels and buttons of CLOB columns for UPDATE
        // ----------------------------------------------------
        clobLabels = new ArrayList<>();
        clobButtons = new ArrayList<>();
        clobTypes = new String[allNumOfCols];

        for (int idx = 0; idx < allNumOfCols; idx++) {
            if ("CLOB".equals(allColTypes.get(idx))) {
                if ("CLOB".equals(allColTypes.get(idx))) {
                    clobTypes[idx] = "CLOB";
                }
                // Column name, type, size CCSID
                JLabel label = new JLabel(allColNames.get(idx) + "   " + clobTypes[idx] + " (" + allColSizes.get(idx) + ")");
                clobLabels.add(label);

                // Column CLOB Button. Its text is the column name!
                JButton button = new JButton(allColNames.get(idx));
                button.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
                button.setForeground(DIM_RED);
                clobButtons.add(button);

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.WEST;

                // Add column label to the input data panel on the left
                inputDataPanel.add(label, gbc);

                // CLOB buttons are added for UPDATE only!
                if (!addNewRecord) {
                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.WEST;

                    // Add button to the input data panel on the right
                    inputDataPanel.add(button, gbc);
                }
            }
        }

        // Create labels and buttons of BLOB columns for UPDATE
        // ----------------------------------------------------
        blobLabels = new ArrayList<>();
        blobButtons = new ArrayList<>();

        for (int idx = 0; idx < allNumOfCols; idx++) {
            if ("BLOB".equals(allColTypes.get(idx))) {

                // Column name, type and size
                JLabel label = new JLabel(allColNames.get(idx) + "  " + "BLOB" + " (" + allColSizes.get(idx) + ")");
                blobLabels.add(label);

                // Column CLOB Button. Its text is the column name!
                JButton button = new JButton(allColNames.get(idx));
                button.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
                button.setForeground(DIM_RED);
                blobButtons.add(button);

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.WEST;

                // Add column label to the input data panel on the left
                inputDataPanel.add(label, gbc);

                // BLOB buttons are added for UPDATE only!
                if (!addNewRecord) {
                    gbc.gridx = 1;
                    gbc.anchor = GridBagConstraints.WEST;
                    // Add button to the input data panel on the right
                    inputDataPanel.add(button, gbc);
                }
            }
        }

        // Register listeners for all CLOB buttons for UPDATE
        // --------------------------------------------------
        if (!addNewRecord) {
            for (int idx = 0; idx < clobButtons.size(); idx++) {
                int index = idx;
                clobButtons.get(idx).addActionListener(ae -> {
                    // Column name is obtained from the ActionEvent ae (button text)
                    colName = ae.getActionCommand();
                    msg.setText(" ");
                    dataMsgPanel.removeAll();
                    dataMsgPanel.add(msg);
                    updateClob(index);
                });
            }
        }

        // Register listeners for all BLOB buttons for UPDATE
        // --------------------------------------------------
        if (!addNewRecord) {
            for (int idx = 0; idx < blobButtons.size(); idx++) {
                blobButtons.get(idx).addActionListener(ae -> {
                    // Column name is obtained from the ActionEvent ae (button text)
                    colName = ae.getActionCommand();
                    msg.setText(" ");
                    dataMsgPanel.removeAll();
                    dataMsgPanel.add(msg);
                    updateBlob();
                });
            }
        }

        // Build button row panel
        JButton returnButton = new JButton(_return);

        JPanel buttonRow = new JPanel();
        buttonRow.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.add(returnButton);

        // Message panels
        BoxLayout msgLayoutX = new BoxLayout(dataMsgPanel, BoxLayout.X_AXIS);
        dataMsgPanel.setLayout(msgLayoutX);
        dataMsgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dataMsgPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        msg.setText(" ");
        dataMsgPanel.removeAll();
        dataMsgPanel.add(msg);

        // Put data global panel to the scroll pane
        scrollPaneData = new JScrollPane(inputDataPanel);

        // Create and lay out the global data panel        
        dataGlobalPanel = new JPanel();
        BoxLayout dataBoxLayout = new BoxLayout(dataGlobalPanel, BoxLayout.Y_AXIS);
        dataGlobalPanel.setLayout(dataBoxLayout);
        dataGlobalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dataGlobalPanel.add(titleDataPanel);
        dataGlobalPanel.add(buttonRow);
        dataGlobalPanel.add(scrollPaneData);
        dataGlobalPanel.add(dataMsgPanel);

        // Set Return button activity
        // --------------------------
        returnButton.addActionListener(a -> {
            this.dispose();
            buildListWindow(this.condition, this.ordering);
        });
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
     * Refresh the list of rows of the table according to WHERE and ORDER BY modifications (if applicable).
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

        // Re-create the table from the modified data (by add, update, or delete)
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
            stmtText = "SELECT RRN(" + file_member.toUpperCase() + ") as RRN" + actualColumnList
                    + "\n FROM " + library.toUpperCase() + "/" + file_member.toUpperCase();
            if (!whereClause.isEmpty()) {
                stmtText += "\n ";
            }
            stmtText += whereClause;
            if (!orderByClause.isEmpty()) {
                stmtText += "\n ";
            }
            stmtText += orderByClause;
            stmtText += "\n FETCH FIRST " + fetchFirst + " ROWS ONLY FOR READ ONLY";

            // Create statement Scroll insensitive and Concurrent updatable
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

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
                    // System.out.println("rs.getObject(col + 1): " + rs.getObject(col + 1));
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
            //System.out.println("Statement:\n" + stmtText);
            //System.out.println("getData: " + exc.toString());
            // Data is missing
            message.setText(noData + " - " + exc.toString() + "\n");
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
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Display only 2 columns in 1 row
            // - the first is RRN, the second is an explaining text
            numOfRows = 0; // no rows in table
            numOfCols = 2; // one column only
            colNames = new String[2];
            colNames[0] = "!";
            colNames[1] = noDataMember;
            colSizes = new int[2];
            colSizes[0] = 10;
            colSizes[1] = 10; 
            rows = new Object[numOfRows][numOfCols];  // Create a new data array
            message.setForeground(DIM_RED); // red local message
        }  // end catch
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
        jTable.getTableHeader().setForeground(DIM_RED);
        jTable.getTableHeader().setMaximumSize(new Dimension(0, (int)(fontSize*1.8)));
        jTable.getTableHeader().setMinimumSize(new Dimension(0, (int)(fontSize*1.8)));
        jTable.getTableHeader().setPreferredSize(new Dimension(0, (int)(fontSize*1.8)));
        //jTable.getTableHeader().setSize(0, (int)(fontSize*2.5));
        // header height
        jTable.setRowHeight((int)(fontSize*1.8));
        // no reordering of headers and columns
        jTable.getTableHeader().setReorderingAllowed(false);
        // Column model for column rendering and editing
        TableColumnModel tcm = jTable.getColumnModel();
        tc = new TableColumn[numOfCols];
        for (int col = 0; col < numOfCols; col++) {
            // Get table column object from the model
            tc[col] = tcm.getColumn(col);
            //tc[col].setCellRenderer(new ColorColumnRenderer(VERY_LIGHT_BLUE, DIM_RED));
        }
        // Column 0 - RRN - different background and foreground color
        tc[0].setCellRenderer(new ColorColumnRenderer(VERY_LIGHT_BLUE, DIM_BLUE));
        tc[0].setHeaderRenderer(new ColorColumnRenderer(VERY_LIGHT_BLUE, DIM_BLUE));

        // Column 0 - RRN - width will be adjusted to fit the column NAME.
        tc[0].setPreferredWidth((int) (Math.max(colSizes[0], cellFieldFactor * colNames[0].length()) + 5));

        // Other columns width will be adjusted by multiplication with cellFieldFactor
        for (int col = 1; col < numOfCols; col++) {
            int colSize = colSizes[col];
            if (colTypes != null) {
                // If the member of the file exists - columns may be processed
                if (colTypes[col].equals("BINARY") || colTypes[col].equals("VARBINARY")) {
                    colSize *= 2;  // Binary columns are twice as wide
                }
                double maxFieldWidth = cellFieldFactor * Math.max(colSize, colNames[col].length());
                if (maxFieldWidth > maxFldWidth) {
                    maxFieldWidth = maxFldWidth;
                }
                tc[col].setPreferredWidth((int) maxFieldWidth);
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

    JLabel msg = new JLabel();

    /**
     * Perform the prepared statement for InsertRow() and UpdateWholeRow()
     * methods
     *
     * @param stmtText
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected boolean performPreparedStatement(String stmtText) {
        dataMsgPanel.removeAll();
        int col = 0;
        // Check input fields for SQL type conformance
        try {
            pstmt = connection.prepareStatement(stmtText);
            for (col = 1; col < numOfCols; col++) {
                // System.out.println("colTypes[col]: " + colTypes[col]);
                // System.out.println("textLabels[col].getText(): " +
                // textLabels[col].getText());

                switch (colTypes[col]) {
                    case "DECIMAL":
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.DECIMAL);
                        } else {
                            pstmt.setBigDecimal(col, new BigDecimal(textLabels[col].getText()));
                        }
                        break;
                    case "INTEGER":
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.INTEGER);
                        } else {
                            pstmt.setInt(col, Integer.parseInt(textLabels[col].getText()));
                        }
                        break;
                    case "DATE":
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.DATE);
                        } else {
                            pstmt.setDate(col, Date.valueOf(textLabels[col].getText()));
                        }
                        break;
                    case "TIME":
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.TIME);
                        } else {
                            pstmt.setTime(col, Time.valueOf(textLabels[col].getText()));
                        }
                        break;
                    case "TIMESTAMP":
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.TIMESTAMP);
                        } else {
                            pstmt.setTimestamp(col, Timestamp.valueOf(textLabels[col].getText()));
                        }
                        break;
                    case "BINARY":
                    case "VARBINARY":
                        // Binary values are represented by hexadecimal characters.
                        // Field length is therefore twice as long
                        int length;
                        char[] chars;
                        String[] strings;
                        byte[] bytes;
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.BINARY);
                        } else {
                            // Field length for BINARY and VARBINARY is obtained the same
                            // way.
                            length = textLabels[col].getText().length();
                            chars = new char[length];
                            strings = new String[length / 2];
                            bytes = new byte[length / 2];
                            textLabels[col].getText().getChars(0, length, chars, 0);
                            // Pairs of hexa characters are transformed into single bytes
                            for (int idx = 0; idx < length / 2; idx++) {
                                // Two hexa characters are transformed into one byte binary
                                // value
                                strings[idx] = String.valueOf(chars[2 * idx])
                                        + String.valueOf(chars[2 * idx + 1]);
                                bytes[idx] = hexToByte(strings[idx]);
                            }
                            pstmt.setBytes(col, bytes);
                        }
                        break;
                    // Remaining types - (var)char, (var)graphic etc.
                    default:
                        if (textLabels[col].getText().equals(nullMark)) {
                            pstmt.setNull(col, java.sql.Types.OTHER);
                        } // JDBC converts Object to the appropriate SQL type
                        else {
                            pstmt.setObject(col, textLabels[col].getText());
                        }
                }
            }

            // For INSERT CLOB the user supplies a text file for the column
            if (addNewRecord) {
                int descriptorIdx;
                // Reader reader = null;
                for (col = 1; col < clobButtons.size() + 1; col++) {
                    if (numOfCols == 1) // If no normal columns are visible, the CLOB columns are numbered
                    {
                        descriptorIdx = col;
                    } else // If some normal columns are visible, normal and CLOB columns are numbered
                    {
                        descriptorIdx = numOfCols - 1 + col;
                    }
                    pstmt.setString(descriptorIdx, null);
                }
            }

            // For INSERT BLOB the user supplies a binary file for the column
            if (addNewRecord) {
                int descriptorIdx;
                // byte[] bytes = null;
                for (col = 1; col < blobButtons.size() + 1; col++) {
                    if (numOfCols == 1) // If no normal columns are visible, the BLOB columns are numbered
                    {
                        descriptorIdx = col + blobButtons.size();
                    } else // If some normal columns are visible, normal and BLOB columns are numbered
                    {
                        descriptorIdx = numOfCols - 1 + blobButtons.size() + col;
                    }
                    pstmt.setBytes(descriptorIdx, null);
                }
            }

            // Perform the statement (UPDATE or INSERT)
            pstmt.execute();
            pstmt.close();
            return true;
        } // end try
        catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("exc.getClass(): " + exc.getClass());
            String msgText;
            try {
                if (exc.getClass() == Class.forName("java.lang.NumberFormatException")) {
                    msgText = invalidValue + col + " - " + colNames[col] + ".";
                    message.setText(msgText);
                    System.out.println("message a: " + message.getText());
                } else if (exc.getClass() == Class.forName("java.sql.SQLIntegrityConstraintViolationException")) {
                    msgText = sqlError + exc.getLocalizedMessage() + ". ";
                    message.setText(msgText);
                    System.out.println("message b: " + message.getText());
                } else if (exc.getClass() == Class.forName("java.lang.IllegalArgumentException")) {
                    msgText = sqlError + exc.getLocalizedMessage() + ". ";
                    message.setText(msgText);
                    System.out.println("message c: " + message.getText());
                } else if (exc.getClass() == Class.forName("com.ibm.as400.access.AS400JDBCSQLSyntaxErrorException")) {
                    message.setText(sqlError + exc.getLocalizedMessage());
                    System.out.println("message d: " + message.getText());
                } else if (exc.getClass() == Class.forName("java.sql.SQLException")) {
                    msgText = sqlError + exc.getLocalizedMessage() + ". ";
                    message.setText(msgText);
                    System.out.println("message e: " + message.getText());
                } else {
                    msgText = sqlError + exc.getLocalizedMessage() + ". ";
                    message.setText(msgText);
                    System.out.println("message f: " + message.getText());
                }
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
            //System.out.println("message: " + message.getText());
            message.setForeground(DIM_RED); // red
            // Put message in data message panel
            dataMsgPanel.add(message);
            repaint();
            setVisible(true);
            return false;
        }
    }

    /**
     * Update CLOB
     * @param col
     */
    protected void updateClob(int col) {
        colLength = 0;
        String clobType = "";
        // Get column capacity for column name just processed
        for (int in = 0; in < allColNames.size(); in++) {
            if (allColNames.get(in).contains(colName)) {
                //System.out.println("ColSize: " + allColSizes.get(in));
                colCapacity = allColSizes.get(in);
                clobType = clobTypes[in];
            }
        }
        try {
            // UPDATE the CLOB column
            // ----------------------
            // Build SELECT statement for current CLOB column (by colName)
            stmtText = "SELECT " + colName + "\n FROM " + library.toUpperCase() + "/"
                    + file_member.toUpperCase();
            stmtText += " WHERE RRN(" + file_member.toUpperCase() + ") = " + rows[rowIndex][0];
            ResultSet rs;

            // Statement is scrollable, updatable
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            //System.out.println("SELECT: \n" + stmtText);

            // Execute the SELECT statement and obtain the ResulSet rs.
            rs = stmt.executeQuery(stmtText);
            rs.next();

            if (clobType.equals("CLOB")) {
                clob =  rs.getClob(colName);
            } 
            // Build UPDATE statement for current CLOB column (by colName)
            stmtText = "UPDATE " + library + "/" + file_member.toUpperCase() + "  SET " + colName;
            stmtText += " = ? WHERE RRN(" + file_member.toUpperCase() + ") = " + rows[rowIndex][0];
            System.out.println("UPDATE: \n" + stmtText);

            // Prepare the UPDATE statement for the column
            pstmt = connection.prepareStatement(stmtText);

            // If clob reference is not null
            if (clob != null) {
                colLength = clob.length();

                // Update CLOB value between start position and length
                U_ClobUpdate clobUpdate = new U_ClobUpdate(this);
                clobUpdate.createWindow(colName, clob, colStartPos, colLength);

                // Get results of changed text area
                retClobValues = clobUpdate.getReturnedValues();
                // CLOB object returned
                clob = retClobValues.getClob();
                // Start position returned
                colStartPos = retClobValues.getStartPos();
                // Length returned
                colLength = retClobValues.getLength();
                // Message returned
                msg = retClobValues.getMsg();

                if (clob != null) {
                    msg = retClobValues.getMsg();
                    msg.setForeground(DIM_BLUE); // Dim blue
                } else {
                    colLength = 0;
                    msg.setText(colValueNull);
                    msg.setForeground(DIM_BLUE); // Dim blue
                }
                // Set the only prepared parameter in UPDATE statement by the CLOB column value
                pstmt.setClob(1, clob);
            } // If the CLOB reference is null
            else {
                msg.setText(colValueNull);
                msg.setForeground(DIM_BLUE); // Dim blue
            }
            // Close SELECT statement
            stmt.close();
            dataMsgPanel.add(msg);
        } catch (Exception exc) {
            exc.printStackTrace();
        } 
        dataMsgPanel.add(msg);
    }

    /**
     * Update BLOB
     */
    protected void updateBlob() {
        msg.setText(" ");
        try {
            // UPDATE the BLOB column
            // ----------------------
            // Build SELECT statement for current BLOB column
            // (by colName)
            stmtText = "SELECT " + colName + "\n FROM " + library.toUpperCase() + "/"
                    + file_member.toUpperCase();
            stmtText += " WHERE RRN(" + file_member.toUpperCase() + ") = " + rows[rowIndex][0];
            // System.out.println("SELECT: \n" + stmtText);

            // Statement is scrollable, updatable
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            // Execute the SELECT statement and obtain the ResulSet rs.
            ResultSet rs = stmt.executeQuery(stmtText);
            // Read the only row
            rs.next();
            // Get the contents of the BLOB column
            blob = rs.getBlob(colName);

            // Build UPDATE statement for current CLOB column
            // (by colName)
            stmtText = "UPDATE " + library + "/" + file_member.toUpperCase() + "  SET " + colName;
            stmtText += " = ? WHERE RRN(" + file_member.toUpperCase() + ") = " + rows[rowIndex][0];
            //System.out.println("UPDATE: \n" + stmtText);

            // If blob reference is not null - content window is displayed
            if (blob != null) {
                blobLength = blob.length();

                // Call class with the window to update the BLOB value 
                // --------------------------
                U_BlobUpdate blobUpdate = new U_BlobUpdate(this);
                blobUpdate.createWindow(colName, blob, blobLength);
                retBlobValues = blobUpdate.getReturnedValues();
                // Get returned values
                blobReturned = retBlobValues.getBlob();
                blobLength = retBlobValues.getLength();
                msg = retBlobValues.getMsg();
                msg.setForeground(DIM_BLUE); // Dim blue
                if (blobReturned != null) {
                    blobLength = blobReturned.length();
                } else {
                    blobLength = 0;
                    msg.setText(colValueNull);
                }

                // Update the column only if a file was loaded and put into the BLOB.
                // The following special message was sent from U_BlobUpdate if not.
                if (!msg.getText().equals("BLOB was not changed")
                        && !msg.getText().equals("BLOB nebyl změněn")) {
                    // Prepare the UPDATE statement for the column
                    pstmt = connection.prepareStatement(stmtText);
                    // Set the only prepared parameter in UPDATE statement
                    // by the BLOB column value
                    pstmt.setBlob(1, blobReturned);
                    // Perform the UPDATE statement for the column
                    pstmt.executeUpdate();
                }
            } else {
                msg.setText(colValueNull);
            }
            stmt.close();
            msg.setForeground(DIM_BLUE); // Dim blue
            dataMsgPanel.add(msg);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (blobLength > 0) {
                    msg.setForeground(DIM_RED); // Dim red
                    msg.setText(txtLength + blobLength + tooLongForCol + colName + "." + contentNotLoaded);
                } else {
                    // If blob length is <= 0
                    msg.setText(e.getLocalizedMessage());
                    msg.setForeground(DIM_RED); // Dim red
                }
                dataMsgPanel.add(msg);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    /**
     * Data model provides methods to fill data from the source to table cells
     * for display. It is applied every time when any change in data source occurs.
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
        // is pressed or when clicked by a mouse. Double click or pressing a datakey
        // invokes the cell editor method - getTableCellEditorComponent().
        // The method is called at least as many times as is the number of cells in the table.
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
        // display in the table. A change in the data source invokes method getValueAt().
        // The method is called after the cell editor ends its activity.
        @Override
        public void setValueAt(Object obj, int row, int col) {
        }

        // Get class of the column value - it is important for the cell editor
        // could be invoked and could determine e.g. the way of aligning of the text in the cell.
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Class getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        // Determine whicn cells are editable or not
        @Override
        public boolean isCellEditable(int row, int col) {
           return false; // columns cannot be changed
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
