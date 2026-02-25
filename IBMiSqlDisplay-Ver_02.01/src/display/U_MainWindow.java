/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package display;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SystemValue;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Starts SQL update application
 *
 * @author Vladimír Župka 2017
 *
 */
public class U_MainWindow extends JFrame {

    final Color RED_LIGHTER = Color.getHSBColor(0.95f, 0.07f, 1); // red level 2
    final Color DIM_PINK = new Color(170, 58, 128);
    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);

    int windowWidth = 650;
    int windowHeight = 700;


    AS400 remoteServer;
    
    ResourceBundle titles;

    // Localized buttons
    ResourceBundle buttons;
    String run, connect;

    // Localized msgTextAreas
    ResourceBundle locMessages;
    String curDir, noConnection, invalSchema, connLost, closeStart;

    // Localized labels
    String defParApp, adrSvr, usrName, correctPar, library, member, fileSelect, autWin, winWidth,
            winHeight;
    String nullMark, fontSize, fetchFirst, char_set, charSelect;
    String maxFldLen;
    String fileName = "";
    String libraryName;

    Path errPath = Paths.get(System.getProperty("user.dir"), "logfiles", "err.txt");
    Path outPath = Paths.get(System.getProperty("user.dir"), "logfiles", "out.txt");
    OutputStream errStream;
    OutputStream outStream;

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding");
    BufferedReader infile;
    BufferedWriter outfile;
    Container cont = getContentPane();
    
    GridBagLayout gridBagLayout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;

    JPanel titlePanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel dataPanel = new JPanel();
    JPanel globalPanel = new JPanel();

    JLabel title;

    JRadioButton englishButton = new JRadioButton("English");
    JRadioButton czechButton = new JRadioButton("Česky");
    JCheckBox autoSizeButton = new JCheckBox("");

    ArrayList<String> libraries;
    ArrayList<String> fileNames;
    JComboBox<String> fileSelectButton;

    // Initial parameter values - not to be empty when the application is installed
    String language;
    JTextField hostTf = new JTextField();
    JTextField userNameTf = new JTextField();
    JTextField libraryTf = new JTextField();
    JTextField memberTf = new JTextField();
    JTextField charsetTf = new JTextField();
    String autoWindowSize = new String();
    JTextField windowWidthTf = new JTextField();
    JTextField windowHeightTf = new JTextField();
    JTextField nullMarkTf = new JTextField();
    JTextField fontSizeTf = new JTextField();
    JTextField fetchFirstTf = new JTextField();
    JTextField maxFldLengthTf = new JTextField();

    // These labels are NOT localized
    JLabel englishLbl = new JLabel("Application language. Restart the application after change.");
    JLabel czechLbl = new JLabel("Jazyk aplikace. Po změně spusťte aplikaci znovu.");

    // Labels for text fields to localize
    JLabel hostLbl;
    JLabel userNameLbl;
    JLabel librariesLbl;
    JLabel memberLbl;
    JLabel fileSelectLbl;
    JLabel charsetLbl;
    JLabel charSelectLbl;
    JLabel autoSizeLbl;
    JLabel windowWidthLbl;
    JLabel windowHeightLbl;
    JLabel nullMarkLbl;
    JLabel fontSizeLbl;
    JLabel fetchFirstLbl;
    JLabel maxFldLengthLbl;

    Properties properties;
    final String PROP_COMMENT = "Sql Display for IBM i, © Vladimír Župka 2015, 2025";

    // Button for saving data to parameter properties
    JButton connectButton;
    // Button to run the update data table
    JButton runUpdateButton;

    // Messages are in a list
    JList messageList = new JList();

    // The messageList is in scroll pane
    JScrollPane scrollMessagePane = new JScrollPane(messageList);
    ArrayList<String> msgVector = new ArrayList<>();
    String row;
    MessageScrollPaneAdjustmentListenerMax messageScrollPaneAdjustmentListenerMax;
 
   /**
     * Constructor creates the window with application parameters
     */
    U_MainWindow() {
        // Get or set application properties
        // ---------------------------------        
        Properties sysProp = System.getProperties();
        // Menu bar in Mac operating system will be in the system menu bar
        if (sysProp.get("os.name").toString().toUpperCase().contains("MAC")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        try {
            // If "paramfiles" directory doesn't exist, create one
            Path paramfilesPath = Paths.get(System.getProperty("user.dir"), "paramfiles");
            if (!Files.exists(paramfilesPath)) {
                Files.createDirectory(paramfilesPath);
            }
            // If "logfiles" directory doesn't exist, create one
            Path logfilesPath = Paths.get(System.getProperty("user.dir"), "logfiles");
            if (!Files.exists(logfilesPath)) {
                Files.createDirectory(logfilesPath);
            }
            // If "selectfiles" directory doesn't exist, create one
            Path selectfilesPath = Paths.get(System.getProperty("user.dir"), "selectfiles");
            if (!Files.exists(selectfilesPath)) {
                Files.createDirectory(selectfilesPath);
            }
            // If "printfiles" directory doesn't exist, create one
            Path columnfilesPath = Paths.get(System.getProperty("user.dir"), "columnfiles");
            if (!Files.exists(columnfilesPath)) {
                Files.createDirectory(columnfilesPath);
            }

            // Redirect System.err, System.out to log files err.txt, out.txt in directory "logfiles"
            errStream = Files.newOutputStream(errPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            outStream = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintStream errPrintStream = new PrintStream(errStream);
        PrintStream outPrintStream = new PrintStream(outStream);
        // PrintStream console = System.out;
        System.setErr(errPrintStream);
        System.setOut(outPrintStream);
        // System.setOut(console);

        properties = new Properties();
        try {
            // If the Parameters.txt file does not exist, create one with default values.
            if (!Files.exists(parPath)) {
                Files.createFile(parPath);
                properties.setProperty("LANGUAGE", "cs-CZ");
                properties.setProperty("HOST", "192.168.1.10");
                properties.setProperty("USER_NAME", "VZUPKA");
                properties.setProperty("LIBRARY", "QIWS");
                properties.setProperty("FILE", "CENY");
                properties.setProperty("MEMBER", "*FIRST");
                properties.setProperty("CHARSET", "UTF-8");
                properties.setProperty("AUTO_WINDOW_SIZE", "Y");
                properties.setProperty("RESULT_WINDOW_WIDTH", "950");
                properties.setProperty("RESULT_WINDOW_HEIGHT", "890");
                properties.setProperty("NULL_PRINT_MARK", "null");
                properties.setProperty("FONT_SIZE", "12");
                properties.setProperty("FETCH_FIRST", "1000");
                properties.setProperty("MAX_FIELD_LENGTH", "1000");
                properties.setProperty("PAPER_SIZE", "A4");
                properties.setProperty("PRINT_FONT_SIZE", "12");
                properties.setProperty("ORIENTATION", "PORTRAIT");
                properties.setProperty("LEFT_MARGIN", "10");
                properties.setProperty("RIGHT_MARGIN", "10");
                properties.setProperty("TOP_MARGIN", "10");
                properties.setProperty("BOTTOM_MARGIN", "10");

                // Create a new text file in directory "paramfiles"
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            }
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        libraryName = properties.getProperty("LIBRARY");
        fileName = properties.getProperty("FILE");
        memberTf.setText(properties.getProperty("MEMBER"));

        language = properties.getProperty("LANGUAGE");
        Locale currentLocale = Locale.forLanguageTag(language);

        // Get resource bundle classes
        titles = ResourceBundle.getBundle("locales.L_TitleLabelBundle", currentLocale);
        buttons = ResourceBundle.getBundle("locales.L_ButtonBundle", currentLocale);
        locMessages = ResourceBundle.getBundle("locales.L_MessageBundle", currentLocale);

        // Localized titles
        defParApp = titles.getString("DefParApp");
        adrSvr = titles.getString("AdrSvr");
        usrName = titles.getString("UsrName");
        library = titles.getString("Library");
        member = titles.getString("Member");
        fileSelect = titles.getString("FileSelect");
        char_set = titles.getString("Char_set");
        charSelect = titles.getString("CharSelect");

        autWin = titles.getString("AutWin");
        winWidth = titles.getString("WinWidth");
        winHeight = titles.getString("WinHeight");
        nullMark = titles.getString("NullMark");
        fontSize = titles.getString("FontSize");
        fetchFirst = titles.getString("FetchFirst");
        maxFldLen = titles.getString("MaxFldLen");

        // Localized button label
        connect = buttons.getString("Connect");
        run = buttons.getString("Run");

        // Localized messages
        curDir = locMessages.getString("CurDir");
        noConnection = locMessages.getString("NoConnection");
        correctPar = locMessages.getString("CorrectPar");
        invalSchema = locMessages.getString("InvalSchema");
        connLost = locMessages.getString("ConnLost");
        closeStart = locMessages.getString("CloseStart");

        menuBar = new JMenuBar();
        helpMenu = new JMenu("Help");
        helpMenuItemEN = new JMenuItem("Help English");
        helpMenuItemCZ = new JMenuItem("Nápověda česky");

        helpMenu.add(helpMenuItemEN);
        helpMenu.add(helpMenuItemCZ);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar); // In macOS on the main system menu bar above, in Windows on the window menu bar
        // End of constructor
    }

    /**
     *
     */
    protected void createWindow() {        
        // Title of the window
        title = new JLabel(defParApp);

        // Labels for text fields
        hostLbl = new JLabel(adrSvr);
        userNameLbl = new JLabel(usrName);
        librariesLbl = new JLabel(library);
        memberLbl = new JLabel(member);
        fileSelectLbl = new JLabel(fileSelect);
        charsetLbl = new JLabel(char_set);
        charSelectLbl = new JLabel(charSelect);
        autoSizeLbl = new JLabel(autWin);
        windowWidthLbl = new JLabel(winWidth);
        windowHeightLbl = new JLabel(winHeight);
        nullMarkLbl = new JLabel(nullMark);
        fontSizeLbl = new JLabel(fontSize);
        fetchFirstLbl = new JLabel(fetchFirst);
        maxFldLengthLbl = new JLabel(maxFldLen);

        // Labels will have the same colors as background
        englishLbl.setBackground(titlePanel.getBackground());
        czechLbl.setBackground(titlePanel.getBackground());
        hostLbl.setBackground(titlePanel.getBackground());
        userNameLbl.setBackground(titlePanel.getBackground());
        librariesLbl.setBackground(titlePanel.getBackground());
        memberLbl.setBackground(titlePanel.getBackground());
        fileSelectLbl.setBackground(titlePanel.getBackground());
        charsetLbl.setBackground(titlePanel.getBackground());
        charSelectLbl.setBackground(titlePanel.getBackground());
        autoSizeLbl.setBackground(titlePanel.getBackground());
        windowWidthLbl.setBackground(titlePanel.getBackground());
        windowHeightLbl.setBackground(titlePanel.getBackground());
        nullMarkLbl.setBackground(titlePanel.getBackground());
        fontSizeLbl.setBackground(titlePanel.getBackground());
        fetchFirstLbl.setBackground(titlePanel.getBackground());
        maxFldLengthLbl.setBackground(titlePanel.getBackground());

        englishLbl.setFont(englishButton.getFont());
        czechLbl.setFont(englishButton.getFont());
        hostLbl.setFont(englishButton.getFont());
        userNameLbl.setFont(englishButton.getFont());
        librariesLbl.setFont(englishButton.getFont());
        memberLbl.setFont(englishButton.getFont());
        fileSelectLbl.setFont(englishButton.getFont());
        charsetLbl.setFont(englishButton.getFont());
        charSelectLbl.setFont(englishButton.getFont());
        autoSizeLbl.setFont(englishButton.getFont());
        windowWidthLbl.setFont(englishButton.getFont());
        windowHeightLbl.setFont(englishButton.getFont());
        nullMarkLbl.setFont(englishButton.getFont());
        fontSizeLbl.setFont(englishButton.getFont());
        fetchFirstLbl.setFont(englishButton.getFont());
        maxFldLengthLbl.setFont(englishButton.getFont());

        // Register HelpWindow menu item listener
        helpMenuItemEN.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Help English")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlDisplayUserDocEn.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (IOException | URISyntaxException exc) {
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
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiSqlDisplayUserDocCz.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        title.setFont(new Font("SansSerif", Font.PLAIN, 20));

        // Language radio buttons
        englishButton.setMnemonic(KeyEvent.VK_E);
        englishButton.setActionCommand("English");
        englishButton.setSelected(true);
        englishButton.setHorizontalTextPosition(SwingConstants.LEFT);

        czechButton.setMnemonic(KeyEvent.VK_C);
        czechButton.setActionCommand("Česky");
        czechButton.setSelected(false);
        czechButton.setHorizontalTextPosition(SwingConstants.LEFT);

        autoSizeButton.setSelected(false);

        // Button for connection and check parameters
        connectButton = new JButton(connect);

        //connectButton.setPreferredSize(new Dimension(100, 40));
        //connectButton.setMaximumSize(new Dimension(100, 40));
        //connectButton.setMinimumSize(new Dimension(100, 40));

        runUpdateButton = new JButton(run);
        runUpdateButton.setEnabled(false);  // Button is disabled at start

        //runUpdateButton.setPreferredSize(new Dimension(100, 40));
        //runUpdateButton.setMaximumSize(new Dimension(100, 40));
        //runUpdateButton.setMinimumSize(new Dimension(100, 40));

        // Radio and check buttons listeners
        // ---------------------------------
        // Set on English, set off Czech
        englishButton.addActionListener(ae -> {
            englishButton.setSelected(true);
            czechButton.setSelected(false);
            language = "en-US";
            System.out.println(ae.getActionCommand());
            System.out.println(language);
            saveParameters();
        });

        // Set on Czech, set off English
        czechButton.addActionListener(ae -> {
            czechButton.setSelected(true);
            englishButton.setSelected(false);
            language = "cs-CZ";
            System.out.println(ae.getActionCommand());
            System.out.println(language);
            saveParameters();
        });

        // Select or deselect automatic window size
        autoSizeButton.addItemListener(il -> {
            Object source = il.getSource();
            if (source == autoSizeButton) {
                if (autoSizeButton.isSelected()) {
                    autoWindowSize = "Y";
                } else {
                    autoWindowSize = "N";
                }
            }
        });

        // Get parameter properties
        // ------------------------
        // This parameter comes from radio buttons
        language = properties.getProperty("LANGUAGE");
        if (language.equals("en-US")) {
            englishButton.setSelected(true);
            czechButton.setSelected(false);
        } else if (language.equals("cs-CZ")) {
            czechButton.setSelected(true);
            englishButton.setSelected(false);
        }

        // The following parameters are editable
        hostTf.setText(properties.getProperty("HOST"));
        hostTf.setPreferredSize(new Dimension(120, 20));
        hostTf.setMinimumSize(new Dimension(120, 20));
        userNameTf.setText(properties.getProperty("USER_NAME"));
        userNameTf.setPreferredSize(new Dimension(120, 20));
        userNameTf.setMinimumSize(new Dimension(120, 20));
        libraryTf.setText(properties.getProperty("LIBRARY"));
        libraryTf.setPreferredSize(new Dimension(120, 20));
        libraryTf.setMinimumSize(new Dimension(120, 20));
        libraryTf.setForeground(Color.BLACK);

        memberTf.setPreferredSize(new Dimension(120, 20));
        memberTf.setMinimumSize(new Dimension(120, 20));
        charsetTf.setText(properties.getProperty("CHARSET"));
        charsetTf.setPreferredSize(new Dimension(160, 20));
        charsetTf.setMinimumSize(new Dimension(160, 20));

        // String "Y" or "N"
        autoWindowSize = properties.getProperty("AUTO_WINDOW_SIZE");
        //
        windowWidthTf.setText(properties.getProperty("RESULT_WINDOW_WIDTH"));
        windowHeightTf.setText(properties.getProperty("RESULT_WINDOW_HEIGHT"));
        nullMarkTf.setText(properties.getProperty("NULL_MARK"));
        fontSizeTf.setText(properties.getProperty("FONT_SIZE"));
        fetchFirstTf.setText(properties.getProperty("FETCH_FIRST"));
        maxFldLengthTf.setText(properties.getProperty("MAX_FIELD_LENGTH"));

        // Automatic size of the window with script results
        if (autoWindowSize.equals("Y")) {
            autoSizeButton.setSelected(true);
        } else {
            autoSizeButton.setSelected(false);
        }
        autoSizeButton.setHorizontalTextPosition(SwingConstants.LEFT);

        // Prepare file selection combo box
        // --------------------------------
        // Create file selection combo box filled by file names
        fileSelectButton = new JComboBox();
        fileSelectButton.setEditable(true);
        fileSelectButton.setPreferredSize(new Dimension(140, 20));
        fileSelectButton.addItem(fileName);
        fileSelectButton.setSelectedIndex(0);

        // Build the window
        // ================

        // Build title panel
        // -----------------
        titlePanel.add(title);
        //titlePanel.setBorder(BorderFactory.createLineBorder(DIM_BLUE)); 

        // Build button panel
        // ------------------
        GroupLayout buttonPanelLayout = new GroupLayout(buttonPanel);
        buttonPanelLayout.setAutoCreateGaps(false);
        buttonPanelLayout.setAutoCreateContainerGaps(true);
        buttonPanelLayout.setHorizontalGroup(buttonPanelLayout.createSequentialGroup()
                .addGap(35)
                .addComponent(connectButton)
                .addGap(35)
                .addComponent(runUpdateButton));
        buttonPanelLayout.setVerticalGroup(buttonPanelLayout
                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addGap(35)
                .addComponent(connectButton)
                .addGap(35)
                .addComponent(runUpdateButton));
        buttonPanel.setLayout(buttonPanelLayout);

        // Build data panel
        // ----------------
        dataPanel.setLayout(gridBagLayout);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(2, 2, 2, 2);

        // internal padding of components
        gbc.ipadx = 0; // horizontal
        gbc.ipady = 0; // vertical

        // Place text fields in column 0
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0;
        gbc.gridy = 0;

        dataPanel.add(englishButton, gbc);
        gbc.gridy++;
        dataPanel.add(czechButton, gbc);
        gbc.gridy++;
        dataPanel.add(hostTf, gbc);
        gbc.gridy++;
        dataPanel.add(userNameTf, gbc);
        gbc.gridy++;
        dataPanel.add(autoSizeButton, gbc);
        gbc.gridy++;
        dataPanel.add(windowWidthTf, gbc);
        gbc.gridy++;
        dataPanel.add(windowHeightTf, gbc);
        gbc.gridy++;
        dataPanel.add(nullMarkTf, gbc);
        gbc.gridy++;
        dataPanel.add(fontSizeTf, gbc);
        gbc.gridy++;
        dataPanel.add(fetchFirstTf, gbc);
        gbc.gridy++;
        dataPanel.add(maxFldLengthTf, gbc);
        gbc.gridy++;
        dataPanel.add(libraryTf, gbc);
        gbc.gridy++;
        dataPanel.add(fileSelectButton, gbc);
        gbc.gridy++;
        dataPanel.add(memberTf, gbc);
        gbc.gridy++;

        // Place labels in column 1
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.gridy = 0;

        dataPanel.add(englishLbl, gbc);
        gbc.gridy++;
        dataPanel.add(czechLbl, gbc);
        gbc.gridy++;
        dataPanel.add(hostLbl, gbc);
        gbc.gridy++;
        dataPanel.add(userNameLbl, gbc);
        gbc.gridy++;
        dataPanel.add(autoSizeLbl, gbc);
        gbc.gridy++;
        dataPanel.add(windowWidthLbl, gbc);
        gbc.gridy++;
        dataPanel.add(windowHeightLbl, gbc);
        gbc.gridy++;
        dataPanel.add(nullMarkLbl, gbc);
        gbc.gridy++;
        dataPanel.add(fontSizeLbl, gbc);
        gbc.gridy++;
        dataPanel.add(fetchFirstLbl, gbc);
        gbc.gridy++;
        dataPanel.add(maxFldLengthLbl, gbc);
        gbc.gridy++;
        dataPanel.add(librariesLbl, gbc);
        gbc.gridy++;
        dataPanel.add(fileSelectLbl, gbc);
        gbc.gridy++;
        dataPanel.add(memberLbl, gbc);
        gbc.gridy++;

        // Build Scroll message pane
        // -------------------------
        // This adjustment listener shows the scroll pane at the FIRST MESSAGE.
        messageScrollPaneAdjustmentListenerMax = new MessageScrollPaneAdjustmentListenerMax();

        // List of messages to place into the message scroll pane.
        // Decide what color the message will get.
        messageList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().startsWith("C")) {
                    this.setForeground(DIM_BLUE);
                } else if (value.toString().startsWith("!")) {
                    this.setForeground(DIM_RED);
                } else if (value.toString().startsWith("I")) {
                    this.setForeground(Color.GRAY);
                } else if (value.toString().startsWith("?")) {
                    this.setForeground(DIM_PINK);
                } else {
                    this.setForeground(Color.BLACK);
                }
                return component;
            }
        });

        // Build messageList and put it to scrollMessagePane and panelMessages
        buildMessageList();

        // Scroll pane for message text area
        scrollMessagePane.setBorder(null);
        scrollMessagePane.setPreferredSize(new Dimension(windowWidth, 100));

        // Build global panel
        // ------------------
        GroupLayout globalPanelLayout = new GroupLayout(globalPanel);
        globalPanelLayout.setAutoCreateGaps(false);
        globalPanelLayout.setAutoCreateContainerGaps(false);
        globalPanelLayout.setHorizontalGroup(globalPanelLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(titlePanel)
                .addComponent(dataPanel)
                .addComponent(buttonPanel)
                .addComponent(scrollMessagePane));
        globalPanelLayout.setVerticalGroup(globalPanelLayout.createSequentialGroup()
                .addComponent(titlePanel)
                .addComponent(dataPanel)
                .addComponent(buttonPanel)
                .addComponent(scrollMessagePane));
        globalPanel.setLayout(globalPanelLayout);
        
        cont.add(globalPanel);

        // Libraries text field - listener has the same function as the connect button
        // --------------------
        libraryTf.addActionListener(al -> {
            connectButton.doClick();
        });
        
        // File name combo box - selection from a list  - listener
        // -------------------------------------------
        fileSelectButton.addItemListener(il -> {
            // If the file name list is empty - send message and return
            libraryTf.setBackground(Color.WHITE);            
            if (fileSelectButton.getItemCount() > 0) {  // If the list is not empty
                fileName = fileSelectButton.getSelectedItem().toString();  // get selected item as file name
            } 
            //properties.setProperty("FILE", fileName);  // Store file name in properties
            memberTf.setText("*FIRST");  // and set member field to *FIRST
        });        
        
        // Connect/check button - listener
        // --------------------    
        connectButton.addActionListener(al -> {
            library = libraryTf.getText().toUpperCase();
            if (connectServer() & checkLibrary() & checkOtherParameters()) {
                runUpdateButton.setEnabled(true);
            } else {
                runUpdateButton.setEnabled(false);
            }
        });
        
        // Run button - listener
        // ----------
        runUpdateButton.addActionListener(al -> {
            library = libraryTf.getText().toUpperCase();
            saveParameters();
            if (checkLibrary() & checkOtherParameters()) {
                U_DataTable dataTable = new U_DataTable(U_ConnectDB.connection);
                dataTable.buildListWindow(dataTable.condition, dataTable.ordering);
            } else {
                runUpdateButton.setEnabled(false);
            }
        });
        
        // Make window visible - conclusion of window building
        // -------------------
        setSize(windowWidth, windowHeight);
        setLocation(200, 40);
        //pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Methods and classes
    // ===================
    
    /**
     * Check non-library parameters
     */
    private boolean checkOtherParameters() {
        // Check numeric values of some parameters
        libraryTf.setText(libraryTf.getText().toUpperCase());
        if (memberTf.getText().isEmpty()) {
            memberTf.setText("*FIRST");
        }
        if (!checkNumber(windowWidthTf)) {
            return false;
        }
        if (!checkNumber(windowHeightTf)) {
            return false;
        }
        if (!checkNumber(fontSizeTf)) {
            return false;
        }
        if (!checkNumber(fetchFirstTf)) {
            return false;
        }
        if (!checkNumber(maxFldLengthTf)) {
            return false;
        }
        return true;
    }
    
    /**
     * Save Parameters as properties
     */
    protected void saveParameters() {
        // Set properties with input values
        properties.setProperty("LANGUAGE", language);
        properties.setProperty("HOST", hostTf.getText());
        properties.setProperty("USER_NAME", userNameTf.getText());
        properties.setProperty("LIBRARY", libraryTf.getText().toUpperCase());
        properties.setProperty("FILE", fileName);
        properties.setProperty("MEMBER", memberTf.getText());
        properties.setProperty("CHARSET", charsetTf.getText());
        properties.setProperty("AUTO_WINDOW_SIZE", autoWindowSize);
        properties.setProperty("RESULT_WINDOW_WIDTH", windowWidthTf.getText());
        properties.setProperty("RESULT_WINDOW_HEIGHT", windowHeightTf.getText());
        properties.setProperty("NULL_MARK", nullMarkTf.getText());
        properties.setProperty("FONT_SIZE", fontSizeTf.getText());
        properties.setProperty("FETCH_FIRST", fetchFirstTf.getText());
        properties.setProperty("MAX_FIELD_LENGTH", maxFldLengthTf.getText());
        // Store properties to output file
        try {
            // Store properties
            try (BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding))) {
                // Store properties
                properties.store(outfile, PROP_COMMENT);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }        
    }
    
    /**
     * Check correctness of the library and set file list to the combo box
     *
     * @return
     */
    protected boolean connectServer() {
        libraryTf.setText(libraryTf.getText().toUpperCase());
        libraryTf.setForeground(Color.BLACK);
        libraryTf.setBackground(Color.WHITE);
        msgVector.removeAll(msgVector);
                            
        // First, check or get connection to the server              
        // --------------------------------------------
        // Try ping on the server if connection is possible. If not, return false.
        AS400JPing pingObj = new AS400JPing(hostTf.getText());
        long timeoutMilliscconds = 8000;
        pingObj.setTimeout(timeoutMilliscconds);
        if (!pingObj.ping()) {
            row = "! Server " + hostTf.getText() + " timed out "
                    + timeoutMilliscconds + " milliseconds.";
            msgVector.add(row);
            showMessages();
            return false;
        }
        
        // Connect DB server 
        // -----------------
        if (U_ConnectDB.connection == null) {
            U_ConnectDB.connection = U_ConnectDB.connect();
        } 
        // Connect server to report informational messages
        // ------------------------
        remoteServer = new AS400(hostTf.getText(), userNameTf.getText());
        try {
            // Connect FILE service in advance
            remoteServer.connectService(AS400.FILE);
            // Information of the directory where the application resides
            row = "I " + curDir + System.getProperty("user.dir") ;
            msgVector.add(row);
            // Obtain and show he system value QCCSID
            SystemValue sysVal = new SystemValue(remoteServer, "QCCSID");
            row = "I System value QCCSID = " + sysVal.getValue() + ".";
            msgVector.add(row);
            // Get server job (NUMBER, USER, NAME)
            ProgramCall pgm = new ProgramCall(remoteServer);
            pgm.setThreadSafe(true);  // Indicates that the program is to be run on-thread.

            Job serverJob = pgm.getServerJob(); 
            serverJob.getSubsystem(); 
            row = "I Subsystem = " + serverJob.getSubsystem() + ",  Job = " +serverJob + ".";
            msgVector.add(row);
            showMessages();
            
        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                ObjectDoesNotExistException | RequestNotSupportedException | IOException | InterruptedException exc) {
            exc.printStackTrace();
            row = "! " + connLost + hostTf.getText()  + " - " + exc.toString();
            msgVector.add(row);
            row = "! " + closeStart + ".";
            msgVector.add(row);
            showMessages();
            // Change cursor to default
            setCursor(Cursor.getDefaultCursor());
            // Remove setting the last element of messages
            scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            return false;
        }
        // Connect database server 
        // -----------------------
        if (U_ConnectDB.connection == null) {
            U_ConnectDB.connection = U_ConnectDB.connect();
        }
        return true;
    }
    
    /**
     * Check library name
     * @return 
     */
    protected boolean checkLibrary() {
                            
        // First, check or get connection to the server              
        // --------------------------------------------
        // Try ping on the server if connection is possible. If not, return false.
        AS400JPing pingObj = new AS400JPing(hostTf.getText());
        long timeoutMilliscconds = 8000;
        pingObj.setTimeout(timeoutMilliscconds);
        if (!pingObj.ping()) {
            row = "! Server " + hostTf.getText() + " timed out "
                    + timeoutMilliscconds + " milliseconds.";
            msgVector.add(row);
            showMessages();
            return false;
        }

        // Connect DB server
        // -----------------
       if (U_ConnectDB.connection == null) {
            U_ConnectDB.connection = U_ConnectDB.connect();
        } 
        try {
            DatabaseMetaData dmd = U_ConnectDB.connection.getMetaData();
            ResultSet rs;
            // First check if the library name is in the list of libraries (schemas)
            libraries = new ArrayList<>();
            rs = dmd.getSchemas();
            while (rs.next()) {
                libraries.add(rs.getString(1));  // next library from the database
            }
            rs.close();
            libraryTf.setBackground(Color.WHITE);
            if (libraries.indexOf(libraryTf.getText().toUpperCase()) == -1) {
                // If the library name is not found in the list of libraries or is empty
                //  report message and color the field.
                row = "! " + invalSchema + " - " + library;
                msgVector.add(row);
                showMessages();
                libraryTf.setBackground(RED_LIGHTER);
                return false;
            } else {
                // If the library is correct, create list of files (tables and views)
                // - physical and logical DATA files (with a single member *FIRST)
                // - from the schema - library - specified in Parameters
                fileNames = new ArrayList<>();
                // Build SELECT statement
                String stmtText = "SELECT  TABLE_NAME"
                        + " from QSYS2.SYSFILES"
                        + " where TABLE_SCHEMA = '" + libraryTf.getText() + "'"
                        + " and FILE_TYPE = 'DATA'";
                Statement stmt = U_ConnectDB.connection.createStatement();
                ResultSet resSet = stmt.executeQuery(stmtText);
                while (resSet.next()) {
                    fileNames.add(resSet.getString("TABLE_NAME"));
                }
                resSet.close();            
                // Fill combo box with the list of file names
                fileSelectButton.removeAllItems();
                for (int idx = 0; idx < fileNames.size(); idx++) {
                    fileSelectButton.addItem(fileNames.get(idx));
                }
                // If the library names equal select the old file name from application parameters
                if (library.equals(libraryName)) {
                    fileSelectButton.setSelectedItem(properties.getProperty("FILE"));
                } else {
                    // Else select the first name from the list                
                    fileSelectButton.setSelectedIndex(0);
                    // Replace library name in the field
                    libraryTf.setText(library);
                    // Make library names equal
                    libraryName = library;
                }
                libraryTf.setText(libraryTf.getText().toUpperCase());
                memberTf.setText(memberTf.getText().toUpperCase());
                return true;
            }
        } catch (SQLException sqle) {
            // No connection to the database server
            sqle.printStackTrace();
            row = "! " + noConnection + " - " + sqle.getLocalizedMessage();
            msgVector.add(row);
            row = "! " + closeStart + ". - " + sqle.getLocalizedMessage();
            msgVector.add(row);
            showMessages();
            return false;
        }
    }
    
    /**
     * Check input text field if it is numeric
     *
     * @param tf
     * @return boolean
     */
    protected boolean checkNumber(JTextField tf) {
        try {
            tf.setBackground(Color.WHITE);
            Integer.valueOf(tf.getText());
        } catch (NumberFormatException exc) {
            tf.setBackground(RED_LIGHTER);
            row = "? " + correctPar + " - " + exc.getLocalizedMessage();
            msgVector.add(row);
            showMessages();
            return false;
        }
        return true;
    }
    
    /**
     * Build message list.
     */
    protected void buildMessageList() {
        messageList.setSelectionBackground(Color.WHITE);
        ArrayList<String> newMsgVector = new ArrayList<>();
        for (String message : msgVector) {
            newMsgVector.add(message);
            msgVector = newMsgVector;
        }
        // Fill message list with elements of the array list
        messageList.setListData(msgVector.toArray(new String[msgVector.size()]));
        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }
    
    /**
     * Show messages
     */
    protected void showMessages() {
        scrollMessagePane.getVerticalScrollBar()
                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        buildMessageList();
        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }

    /**
     * Adjustment listener for MESSAGE scroll pane.
     */
    class MessageScrollPaneAdjustmentListenerMax implements AdjustmentListener {

        @Override
        public void adjustmentValueChanged(AdjustmentEvent ae) {
            // Set scroll pane to the bottom - the last element
            ae.getAdjustable().setValue(ae.getAdjustable().getMaximum());
        }
    }

    /**
     * Disconnect from the server and close the main window.
     */
    class WindowClosing extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            try {
                U_ConnectDB.connection.close();
                U_ConnectDB.disconnect(U_ConnectDB.connection);
            } catch (SQLException exc) {
                exc.getLocalizedMessage();
            }
        }
    }

    /**
     * Main class creates the object of this class and calls method to create the
     * window.
     *
     * @param strings
     * not used
     */
    public static void main(String... strings) {
        try {
            U_MainWindow sqlUpdate = new U_MainWindow();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            sqlUpdate.createWindow();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException exc) {
            exc.printStackTrace();
        }
    }

}
