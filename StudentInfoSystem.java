import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class StudentInfoSystem extends JFrame {
	
	private static final long serialVersionUID = 7411363481203108843L;
	//########BEGIN JFRAME COMPONENTS########
	private final StudentInfoSystem self = this;
	private JMenuBar menuBar;
	private JMenu studentMenu,  //The JMenu objects added to the JFrame.
	              courseMenu,
	              enrollMenu,
	              gradesMenu,
	              reportsMenu;
	private JMenuItem createStudentItem,  //JMenuItems added to the JMenu objects
					  viewUpdateStudentItem,
					  createCourseItem,
					  viewUpdateCourseItem,
					  enrollItem,
					  addGradesItem,
					  viewGradesItem,
					  viewReportsItem;
	private JPanel masterPanel; //masterPanel is the only panel directly added onto the JFrame; to change panels, I only need to change the masterPanel reference.
	//########END JFRAME COMPONENTS########
	
	private final String[] year = {"2016","2015","2014","2013","2012","2011","2010","2009","2008","2007","2006","2005",
								   "2004","2003","2002","2001","2000","1999","1998","1997","1996","1995"},
				  		   semester = {"Spring", "Summer", "Fall", "Winter"};  //String arrays later used by JComboBox objects
	private final Map<String,RandomAccessFile> mapRecordToStream;
	private final int TEXT_SIZE = 30, //All strings are written to and read from a file as char arrays of this size. Including primitive data, a fixed text size ensures all records have fixed length.
					  SC_RECORD_SIZE = 186; //int: 4 bytes + 3 * char[30]: 180 bytes + short: 2 bytes
	private final Map<String,Integer> OFFSET = new HashMap<String,Integer>() { //These offsets are used to calculate student and course IDs. 
		private static final long serialVersionUID = 1L;                       
		{ put("Student",90000); put("Course",1000); }  //Student and Course IDs respectively begin at these offsets when their respective files contain no records.
	};
	public StudentInfoSystem() {
		final RandomAccessFile[] stream = new RandomAccessFile[3];
		
		setTitle("Student Information System");
		
		setMinimumSize(new Dimension(800,700));  //A minimum size for the JFrame, so the title always remains visible even when masterPanel changes.
		
		buildMenuBar();
		
		buildHomePanel();   //Set masterPanel to home panel
		
		setVisible(true);
		
		/*In the following try-catch block we open our three streams for each of the record files. These streams stay open until program termination.
		 * Note that the streams are opened as "rw", so if the files for these streams do not already exist, they will be created.
		 * If the files for these streams already exist, they will not be overwritten.
		 */
		try {
			stream[0] = new RandomAccessFile("StudentFile.dat","rw");
			stream[1] = new RandomAccessFile("CourseFile.dat","rw");
			stream[2] = new RandomAccessFile("EnrollmentFile.dat","rw");
		} catch (FileNotFoundException exception) {
			JOptionPane.showMessageDialog(null, "Error: " + exception);
		}
		
		//Easy mapping of a record type to the stream of the relevant file.
		mapRecordToStream = new HashMap<String,RandomAccessFile>() { 
			private static final long serialVersionUID = 4796322824319795839L;
			{ put("Student", stream[0]); put("Course",stream[1]); put("Enroll",stream[2]); }
		};
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		//The user is prompted to make sure they actually want to close the application, and before closing all streams are also closed.
		addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		    	int confirmExit = JOptionPane.showOptionDialog(
		             null, "Are you sure you want to close the application?", 
		             "Exit Confirmation", JOptionPane.YES_NO_OPTION, 
		             JOptionPane.QUESTION_MESSAGE, null, null, null);
		        if (confirmExit == 0) {
					try {
						for(int stm = 0; stm < 3; stm++)
							stream[stm].close();
					} catch (IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
		           System.exit(0);
		        }
		    }
		});
	}
	
	//To change the masterPanel, first remove masterPanel from the JFrame, link it to a new reference, and add it back onto the JFrame.
	private void switchMasterPanelTo(JPanel panel) {
		remove(masterPanel);
		masterPanel = panel;
		add(masterPanel, BorderLayout.CENTER);
		revalidate();
		pack();
		setLocationRelativeTo(null);
	}
	
	//Builds the menu bar
	private void buildMenuBar() {
		menuBar = new JMenuBar();
		buildStudentMenu();
		buildCourseMenu();
		buildEnrollMenu();
		buildGradesMenu();
		buildReportsMenu();
		menuBar.add(studentMenu);
		menuBar.add(courseMenu);
		menuBar.add(enrollMenu);
		menuBar.add(gradesMenu);
		menuBar.add(reportsMenu);
		setJMenuBar(menuBar);
	}
	/*
	 * The following build__Menu methods instantiate the necessary JMenu and their child JMenuItems. The JMenuItems are chained to listeners, and added to the JMenu.
	 * Note a pattern: when a listener is instantiated once, I use an anonymous class.
	 *                 when instantiated multiple times, I create a separate inner class.
	 * I follow this pattern for many of the classes in this project in accordance with DRY and reducing the complexity of the code.
	 */
	private void buildStudentMenu() {
		studentMenu = new formattedJMenu("Student");
		studentMenu.setMnemonic(KeyEvent.VK_1);
		createStudentItem = new formattedJMenuItem("Create");
		viewUpdateStudentItem = new formattedJMenuItem("View/Update");
		createStudentItem.addActionListener(new CreateMenuItemListener());
		viewUpdateStudentItem.addActionListener(new ViewUpdateMenuItemListener());
		studentMenu.add(createStudentItem);
		studentMenu.add(viewUpdateStudentItem);
	}
	
	private void buildCourseMenu() {
		courseMenu = new formattedJMenu("Course");
		courseMenu.setMnemonic(KeyEvent.VK_2);
		createCourseItem = new formattedJMenuItem("Create");
		viewUpdateCourseItem = new formattedJMenuItem("View/Update");
		createCourseItem.addActionListener(new CreateMenuItemListener());
		viewUpdateCourseItem.addActionListener(new ViewUpdateMenuItemListener());
		courseMenu.add(createCourseItem);
		courseMenu.add(viewUpdateCourseItem);
	}
	
	private void buildEnrollMenu() {
		enrollMenu = new formattedJMenu("Enroll");
		enrollMenu.setMnemonic(KeyEvent.VK_3);
		enrollItem = new formattedJMenuItem("Enroll");
		enrollItem.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				switchMasterPanelTo(new EnrollPanel());
			}
		});
		enrollMenu.add(enrollItem);
	}
	
	private void buildGradesMenu() {
		gradesMenu = new formattedJMenu("Grades");
		gradesMenu.setMnemonic(KeyEvent.VK_4);
		addGradesItem = new formattedJMenuItem("Add");
		addGradesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchMasterPanelTo(new AddGradesPanel());
			}
		});
		viewGradesItem = new formattedJMenuItem("View");
		viewGradesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchMasterPanelTo(new ViewGradesPanel());
			}
		});
		gradesMenu.add(addGradesItem);
		gradesMenu.add(viewGradesItem);
	}
	
	private void buildReportsMenu() {
		reportsMenu = new formattedJMenu("Reports");
		reportsMenu.setMnemonic(KeyEvent.VK_5);
		viewReportsItem = new formattedJMenuItem("View");
		viewReportsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchMasterPanelTo(new ReportPanel());
			}
		});
		reportsMenu.add(viewReportsItem);
	}
	
	private class formattedJMenu extends JMenu {
		private static final long serialVersionUID = -8388142119166461544L;
		{this.setFont(new Font("Serif",Font.BOLD,25));}
		public formattedJMenu(String title) {
			super(title);
		}
	}
	
	private class formattedJMenuItem extends JMenuItem {
		private static final long serialVersionUID = 3051424999489951006L;
		{this.setFont(new Font("Serif",Font.PLAIN,20));}
		public formattedJMenuItem(String title) {
			super(title);
		}
	}
	
	
	private class CreateMenuItemListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			switchMasterPanelTo(e.getSource() == createStudentItem ? new CreatePanel("Student") : new CreatePanel("Course"));
		}
	}
	
	private class ViewUpdateMenuItemListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			switchMasterPanelTo(e.getSource() == viewUpdateStudentItem ? new ViewUpdatePanel("Student") : new ViewUpdatePanel("Course"));
		}
	}
	
	//This method creates the first instance of the HomePanel and links it to the masterPanel when the application begins. Only to be called once.
	private void buildHomePanel() {
		masterPanel = new HomePanel();
		add(masterPanel);
		revalidate();
		pack();
		setLocationRelativeTo(null);
	}
	
	//The home panel 
	private class HomePanel extends JPanel {
		private static final long serialVersionUID = 8255640781278776089L;
		private GridBagConstraints c;
		private LabelPanel welcomeLabel;
		private LabelPanel[] directionLabels;
		private String welcomeText = "Welcome to the Student Information System.";
		private String[] directionText = {"Students: Create or view/update a student.",
				"Courses: Create or view/update a course.", "Enroll: Enroll a student in a course.",
				"Grades: Add or view a grade.","Reports: View all students enrolled in a course for a given year and semester."};
		public HomePanel() {
			int length = directionText.length;
			setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.weighty = 1;
			c.anchor = GridBagConstraints.CENTER;
			welcomeLabel = new LabelPanel(welcomeText);
			directionLabels = new LabelPanel[length];
			for(int index = 0; index < length; index++) {
				directionLabels[index] = new LabelPanel("(alt + " + (index + 1) + ") for " + directionText[index]);
			}
			c.gridy = 0;
			add(welcomeLabel,c);
			for(LabelPanel label : directionLabels) {
				++c.gridy;
				add(label,c);
			}
			pack();
		}
	}
	
	/*TemplatePanel provides a basic set of features that are inherited in all future panels:
	 *      -GridBagLayout is used to provide the greatest personal degree of control over design.
	 *      -Functions addAsNewRow(...), addAsMatrix(...) and addToCenter(...) allow for multiple components to be added exactly as stated 
	 *       without needing to deal directly with GridBagConstraints every time.
	 *      -There is a home button that allows the user to go back to the home panel/page, and a reset button that resets the panel using resetPanel().
	 *      -Function resetPanel() provides an easy, general way to clear or reset all the fields in a subclass instance at once.
	 *       resetPanel() uses reflection to create a new instance of the subclass object that originally called said method, and
	 *       masterPanel is then switched to this new instance, destroying the old reference.
	 *      -Note that resetPanel() requires that the subclass have a no-arg default constructor so that it can be found by reflection.
	 *       The constructors of inner classes implicitly put the instance of their outer class in their first argument, much like other languages like Python.
	 */
	private abstract class TemplatePanel extends JPanel {
		private static final long serialVersionUID = 702205217604505954L;
		protected final GridBagConstraints c;
		private ButtonPanel homeButtonPanel,
						    resetButtonPanel;
		protected LabelPanel purpose,
							 userGuide;
		public TemplatePanel(String title, String purposeText, String userGuideText) {
			setLayout(new GridBagLayout());
			setBorder(BorderFactory.createTitledBorder(null,title,TitledBorder.CENTER, TitledBorder.TOP,new Font("Monospaced",Font.BOLD,30)));
			c = new GridBagConstraints();
			c.ipadx = 250;
			c.ipady = 20;
			homeButtonPanel = new ButtonPanel("Go to Home (alt+H)");
			homeButtonPanel.button.setMnemonic(KeyEvent.VK_H);
			homeButtonPanel.button.setToolTipText("Go back to the home page.");
			homeButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					switchMasterPanelTo(new HomePanel());
				}
			});
			resetButtonPanel = new ButtonPanel("Reset Fields (alt+R)");
			resetButtonPanel.button.setMnemonic(KeyEvent.VK_R);
			resetButtonPanel.button.setToolTipText("Reset all fields");
			resetButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					resetPanel();
				}
			});
			purpose = new LabelPanel(purposeText);
			userGuide = new LabelPanel(userGuideText);
			addAsNewRow(homeButtonPanel, resetButtonPanel);
			addToCenter(purpose, userGuide);
		};
		@Override
		public Component add(Component comp) {
			add(comp,c);
			return comp;
		}
		public Component[] addAsNewRow(Component... comps) {
			++c.gridy;
			for(Component comp: comps) {
				add(comp);
			}
			return comps;
		}
		public Component[] addAsMatrix(int rows, int columns, Component... comps) {
			if(comps.length != (rows * columns)) 
				throw new IllegalArgumentException("Components must fill matrix of dimension " + rows + "x" + columns + ".");
			int index;
			for(int r = 0; r < rows; r++) {
				++c.gridy;
				index = r * columns;
				for(int col = 0; col < columns; col++) {
					add(comps[index + col]);
				}
			}
			return comps;
		}
		public Component[] addToCenter(Component... comps) {
			int tmp = c.gridwidth;
			c.gridwidth = 2;
			for(Component comp : comps)
				addAsNewRow(comp);
			c.gridwidth = tmp;
			return comps;
		}
		public void resetPanel() {
			try {
				switchMasterPanelTo((JPanel) this.getClass().getConstructor(StudentInfoSystem.class).newInstance(self));
			} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException exception) {
				JOptionPane.showMessageDialog(null, "Error: " + exception);
			}
		}
	}
	
	/*
	 * SCPanel is the superclass of the CreatePanel and ViewUpdatePanel classes that allow the user to create and view/update a student or course.
	 *      -Fields common to both classes are abstracted to this superclass.
	 *      -resetPanel() is overridden to accommodate the constructors of CreatePanel and ViewUpdatePanel.
	 */
	private abstract class SCPanel extends TemplatePanel {
		private static final long serialVersionUID = -2995881420373246654L;
		protected TextPanel[] textPanels = new TextPanel[3];
		protected ComboBoxPanel cbPanel;
		protected String[] studentTextField = {"Name","Address","Age"},
						 courseTextField = {"Title","Description","Room"},
						 studentYear = {"Freshman","Sophomore","Junior","Senior"},
						 major = {"Engineering","Informatics","Social Sciences","Physical Sciences"};
		protected int[] textFieldSize = {TEXT_SIZE,TEXT_SIZE,3};
		private String type;
		public SCPanel(String type,String titleAction, String purposeAction, String userGuideText) {
			super(titleAction + type, purposeAction + type.toLowerCase() + ".", userGuideText);
			this.type = type;
			for(int index = 0; index < textPanels.length; index++) 
				textPanels[index] = new TextPanel(type == "Student" ? studentTextField[index] : courseTextField[index],textFieldSize[index]);
			cbPanel = new ComboBoxPanel(type == "Student" ? "Year" : "Major", type == "Student" ? studentYear : major);
		}
		@Override
		public void resetPanel() {
			try {
				switchMasterPanelTo((JPanel) this.getClass().getConstructor(StudentInfoSystem.class,String.class).newInstance(self,type));
			} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException exception) {
				JOptionPane.showMessageDialog(null, "Error: " + exception);
			}
		}
	}
	
	/*
	 * This panel allows the user to create a student or course, depending on the argument passed to the constructor.
	 * When the user presses the create button, its listener is activated. Inside the listener, the text fields input by the user are validated by
	 * validateSCFields(...):
	 * 		if all the fields are valid a new student or course record is written to its respective file. The ID of the new student/course is displayed to the
	 * 		user by message dialog.
	 * 		otherwise, the user is informed that a field is incorrect by a message dialog pop-up, and the incorrect field has focus set upon it.
	 * 		If multiple fields are incorrect, then the highest priority field is highlighted, where priority is by the index of the offending textPanel.
	 */
	private class CreatePanel extends SCPanel {
		private static final long serialVersionUID = 6780901698837407809L;
		private ButtonPanel createButtonPanel;
		public CreatePanel(final String type) {
			super(type,"Create ", "Create a new ", "Fill out the fields below and click \"Create " + type + "\".");
			createButtonPanel = new ButtonPanel("Create " + type + " (alt+C)");
			createButtonPanel.button.setMnemonic(KeyEvent.VK_C);
			createButtonPanel.button.setToolTipText("Create a " + type);
			createButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String[] data = new String[4];
					String[] reason = {" cannot be empty", " cannot be empty", " must be an integer between " + (type == "Student" ? "16 and 122" : "1 and 999")};
					int failIndex = validateSCFields(textPanels,data,type);
					try {
						if(failIndex == -1) {
							for(int index = 0; index < 3; index++)
								data[index] = textPanels[index].textField.getText();
							data[3] = cbPanel.getSelectedItem();
							JOptionPane.showMessageDialog(null, "New " + type + " Created.\n Your " + type + " ID is: " + writeSCRecord(data, type) + ".\nRemember this ID!");
							resetPanel();
						}
						else {
							JOptionPane.showMessageDialog(null, (type == "Student" ? studentTextField[failIndex] : courseTextField[failIndex]) + reason[failIndex] + ".");
							textPanels[failIndex].textField.requestFocusInWindow();
						}
					} catch (IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
				}
			});
			addAsMatrix(2, 2, textPanels[0], textPanels[1], textPanels[2], cbPanel);
			addToCenter(createButtonPanel);
		}
	}
	
	/*
	 * This panel allows a user to view and update an existing student or course record. 
	 * The user first searches for a record by ID: 
	 * 		if the record exists, its fields are displayed, 
	 * 		otherwise, the user is informed by a message dialog that the record does not exist.
	 * The user can then update the fields, and update the record with the update button.
	 * Like CreatePanel, the text fields are validated by validateSCRecord(...):
	 * 		if the fields are valid, the student or course record is updated in its respective file.
	 * 		otherwise, the user is informed that a field is incorrect by a message dialog pop-up, and the incorrect field has focus set upon it.
	 * 		If multiple fields are incorrect, then the highest priority field is highlighted, where priority is by the index of the offending textPanel.
	 */
	private class ViewUpdatePanel extends SCPanel {
		private static final long serialVersionUID = 6780901698837407809L;
		private TextPanel IDPanel;
		private ButtonPanel searchButtonPanel,
						    updateButtonPanel;
		private int ID;
		public ViewUpdatePanel(final String type) {
			super(type,"View/Update ", "View or update an existing ", "Search for a " + type.toLowerCase() + " by ID, update any necessary fields, and then click \"Update " + type + "\".");
			IDPanel = new TextPanel(type + " ID",8);
			for(TextPanel tp : textPanels)
				tp.textField.setEnabled(false);
			cbPanel.comboBox.setEnabled(false);
			searchButtonPanel = new ButtonPanel("Search (alt+S)"); 
			searchButtonPanel.button.setMnemonic(KeyEvent.VK_S);
			searchButtonPanel.button.setToolTipText("Search for a " + type);
			updateButtonPanel = new ButtonPanel("Update " + type + " (alt+E)");
			updateButtonPanel.button.setMnemonic(KeyEvent.VK_E);
			updateButtonPanel.setToolTipText("Update the " + type);
			updateButtonPanel.button.setEnabled(false);
			searchButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						ID = Integer.parseInt(IDPanel.textField.getText());
						String[] textData = readTextFields(ID,type);
						if(textData != null) {
							for(int index = 0; index < 3; index++) {
								textPanels[index].textField.setText(textData[index]);
								textPanels[index].textField.setEnabled(true);
							}
							String userInputCBField = textData[3];
							String[] cbFields = (type == "Student" ? studentYear : major);
							for(int index = 0; index < cbFields.length; index++) {
								if(userInputCBField.equals(cbFields[index])) {
									cbPanel.comboBox.setSelectedIndex(index);
									break;
								}
							}
							cbPanel.comboBox.setEnabled(true);
							updateButtonPanel.button.setEnabled(true);
						}
						else {
							JOptionPane.showMessageDialog(null, type + " Not Found.");
						}
					} catch (NumberFormatException exception) {
						JOptionPane.showMessageDialog(null,"Error: ID must be a number.");
						IDPanel.textField.requestFocusInWindow();
					} catch (IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
				}
			});
			updateButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String[] data = new String[4];
					String[] reason = {" cannot be empty", " cannot be empty", " must be an integer between " + (type == "Student" ? "16 and 122" : "1 and 999")};
					int failIndex = validateSCFields(textPanels,data,type);
					try {
						if(failIndex == -1) {
							for(int index = 0; index < 3; index++)
								data[index] = textPanels[index].textField.getText();
							data[3] = cbPanel.getSelectedItem();
							updateTextFields(ID,data,type);
							JOptionPane.showMessageDialog(null, type + " updated.");
							resetPanel();
						}
						else {
							JOptionPane.showMessageDialog(null, (type == "Student" ? studentTextField[failIndex] : courseTextField[failIndex]) + reason[failIndex] + ".");
							textPanels[failIndex].textField.requestFocusInWindow();
						}
					} catch (IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
				}
			});
			addToCenter(IDPanel, searchButtonPanel);
			addAsMatrix(2, 2, textPanels[0], textPanels[1], textPanels[2], cbPanel);
			addToCenter(updateButtonPanel);
		}
	}
	
	/*
	 * This method validates the text fields of CreatePanel and ViewUpdatePanel objects. Only text fields are validated because
	 *  a JComboBox field cannot return invalid input. The first two fields are merely validated to be non-empty, but the third field is numeric
	 *  so it is checked to be a number within a valid range.
	 *  ---------The index of the first invalid field is returned.-----------
	 */
	public int validateSCFields(TextPanel[] textPanels, String[] data, String type) {
		if(type != "Student" && type != "Course") throw new IllegalArgumentException("Function only to be used for validating Student and Course fields.");
		if(textPanels.length != 3 || data.length != 4) throw new IllegalArgumentException("First argument must be of length three, second of length four.");
		for(int index = 0; index < 2; index++) {
			data[index] = textPanels[index].textField.getText();
			if(data[index].isEmpty()) {
				return index;
			}
		}
		try {
			short val = Short.parseShort(textPanels[2].textField.getText());
			if(type == "Student" && (val < 16 || val > 122)) return 2;  //Oldest person ever lived to be 122. Maybe you can break the record.
			if(type == "Course" && val < 1) return 2;
		} catch(NumberFormatException exception) {
			System.out.print(exception);
			return 2;
		}
		return -1;
	}
	
	/*
	 * This panel allows a user to enroll a student for a course in a given year and semester. 
	 * All input fields are JComboBoxes that have their selections loaded during instance construction.
	 * The year and semester fields load the respective global variable array for their field, and the studentIDs and courseIDs
	 * are loaded from their respective file using the getIDsFromSCFile(...) method.
	 * When the user clicks on enrollButton to enroll a student for a course, the listener passes the input field data to writeEnrollRecord(...), which:
	 *      -returns false if the enroll record, with exact year, semester, student ID and courseID already exists
	 *      -returns true if the enroll record does not already exist, ---writing to the enroll record file.---
	 * If writeEnrollRecord(...) returns true, the user is informed by message dialog that the record has been written.
	 * otherwise, the user is informed by message dialog that the record already exists.
	 */
	private class EnrollPanel extends TemplatePanel {
		private static final long serialVersionUID = 1L;
		private ComboBoxPanel studentIDPanel,
							  yearPanel,
					          semesterPanel,
					          courseIDPanel;
		private ButtonPanel enrollButtonPanel;
		public EnrollPanel() {
			super("Enroll Student", "Enroll a student in a course.", "Fill out the fields below and click \"Enroll Student\".");
			yearPanel = new ComboBoxPanel("Year", year);
			semesterPanel = new ComboBoxPanel("Semester", semester);
			try {
				courseIDPanel = new ComboBoxPanel("Course ID",getIDsFromSCFile("Course"));
				studentIDPanel = new ComboBoxPanel("Student ID",getIDsFromSCFile("Student"));
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null,"Error generating Enroll Page.");
				switchMasterPanelTo(new HomePanel());
			}
			enrollButtonPanel = new ButtonPanel("Enroll Student (alt+E)");
			enrollButtonPanel.button.setMnemonic(KeyEvent.VK_E);
			enrollButtonPanel.button.setToolTipText("Enroll the student in the course.");
			enrollButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						if(writeEnrollRecord(Short.parseShort(yearPanel.getSelectedItem()), semesterPanel.getSelectedItem(), 
								Integer.parseInt(courseIDPanel.getSelectedItem()), Integer.parseInt(studentIDPanel.getSelectedItem())))
							JOptionPane.showMessageDialog(null, "Student enrolled.");	
						else
							JOptionPane.showMessageDialog(null, "Student already enrolled.");
					} catch (NumberFormatException | IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
					resetPanel();
				}
			});
			addAsMatrix(2, 2, yearPanel, semesterPanel, courseIDPanel, studentIDPanel);
			addToCenter(enrollButtonPanel);
		}
	}
	
	/*
	 * GRPanel abstracts common fields and behavior to GradesPanel and ReportsPanel into one superclass.
	 */
	private abstract class GRPanel extends TemplatePanel {
		private static final long serialVersionUID = -40958492250989035L;
		protected ComboBoxPanel yearPanel,
		                      semesterPanel,
		                      courseIDPanel;
		public GRPanel(String title, String purposeText, String buttonName) {
			super(title, purposeText, "Fill out the fields in order from left to right, then click " + "\"" + buttonName + "\".");
			yearPanel = new ComboBoxPanel("Year", year);
			semesterPanel = new ComboBoxPanel("Semester", semester);
			semesterPanel.comboBox.setEnabled(false);
			courseIDPanel = new ComboBoxPanel("Course ID", new String[]{});
			courseIDPanel.comboBox.setEnabled(false);
			addAsNewRow(yearPanel, semesterPanel);
			addAsNewRow(courseIDPanel);
		}
		protected class YearListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				semesterPanel.reload(semester);
				semesterPanel.comboBox.setEnabled(true);
				courseIDPanel.reload(new String[]{});
				courseIDPanel.comboBox.setEnabled(false);
			}
		}
		protected class SemesterListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					String[] courseIDs = getCourseIDsFromEnrollFile(Short.parseShort(((String) yearPanel.comboBox.getSelectedItem())),
							(String) semesterPanel.comboBox.getSelectedItem());
					if(courseIDs.length != 0) {
						courseIDPanel.reload(courseIDs);
						courseIDPanel.comboBox.setEnabled(true);
					}
					else {
						JOptionPane.showMessageDialog(null, "No students are enrolled for courses in given year and semester.");
					}
				} catch (NumberFormatException | IOException exception) {
					JOptionPane.showMessageDialog(null, "Error: " + exception);
				}
			}
		}
	}
	
	/*
	 * GradesPanel abstracts fields and behaviors common to ViewGradesPanel and AddGradesPanel.
	 */
	private abstract class GradesPanel extends GRPanel {
		private static final long serialVersionUID = 5708635953015403811L;
		protected ComboBoxPanel studentIDPanel;
		public GradesPanel(String title, String actionText, String buttonName) {
			super(title, actionText + " an existing enrollment record.", buttonName);
			studentIDPanel = new ComboBoxPanel("Student ID",new String[]{});
			studentIDPanel.comboBox.setEnabled(false);
			add(studentIDPanel);
		}
		protected class CourseIDListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				try {
					studentIDPanel.reload(getStudentIDsFromEnrollFile(Short.parseShort((String) yearPanel.comboBox.getSelectedItem()),
							(String) semesterPanel.comboBox.getSelectedItem(), Integer.parseInt((String) courseIDPanel.getSelectedItem())));
				} catch (NumberFormatException | IOException exception) {
					JOptionPane.showMessageDialog(null, "Error: " + exception);
				}
				studentIDPanel.comboBox.setEnabled(true);
			}
		}
	}
	
	/*
	 * AddGradesPanel allows the user to add a grade to a student's enrollment record.
	 * The user searches for an enrollment record by year, semester, courseID, and studentID.
	 * The fields are selected in order, with the next field becoming enabled when the user has selected a choice
	 * from the previous field. After the user has selected a year and semester, the courseID JComboBox is loaded with all the courseIDs
	 * that match the selected year and semester. 
	 * 		-If no courseIDs are found, the user is informed of this by a message dialog and the courseID JComboBox is not enabled, preventing the user from continuing.
	 * 		-otherwise, the courseID JComboBox becomes enabled.
	 * An existing courseID in the enroll record implies an existing studentID, so when the user selects a courseID the studentID JComboBox is
	 * loaded without needing to check if there are no studentIDs.
	 * Once a studentID has been selected, the user selects a grade and clicks the addButton, whose listener calls ViewAddGrade(...), which:
	 * 		-if a grade has already been added for the given enroll record, returns null
	 * 		-otherwise, adds the grade and returns the grade.
	 * If viewAddGrade(...) is null, the user is informed that the grade has already been added by a message dialog.
	 * otherwise, the user is informed that the grade has been added.
	 */
	private class AddGradesPanel extends GradesPanel {
		private static final long serialVersionUID = -7640541021469470963L;
		private ButtonPanel addButtonPanel;
		private ComboBoxPanel gradePanel;
		public AddGradesPanel() {
			super("Add Grade", "Add a grade to", "Add Grade");
			yearPanel.comboBox.addActionListener(new YearListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					studentIDPanel.reload(new String[]{});
					studentIDPanel.comboBox.setEnabled(false);
					gradePanel.setEnabled(false);
					addButtonPanel.button.setEnabled(false);
				}
			});
			semesterPanel.comboBox.addActionListener(new SemesterListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					studentIDPanel.reload(new String[]{});
					studentIDPanel.comboBox.setEnabled(false);
					gradePanel.setEnabled(false);
					addButtonPanel.button.setEnabled(false);
				}
			});
			courseIDPanel.comboBox.addActionListener(new CourseIDListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					gradePanel.setEnabled(false);
					addButtonPanel.button.setEnabled(false);
				}
			});
			studentIDPanel.comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					gradePanel.reload(new String[]{"A","B","C","D","F","W","P"});
					gradePanel.comboBox.setEnabled(true);
					addButtonPanel.button.setEnabled(false);
				}
			});
			gradePanel = new ComboBoxPanel("Grade",new String[]{});
			gradePanel.comboBox.setEnabled(false);
			gradePanel.comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addButtonPanel.button.setEnabled(true);
				}
			});
			addButtonPanel = new ButtonPanel("Add Grade (alt+A)");
			addButtonPanel.button.setMnemonic(KeyEvent.VK_A);
			addButtonPanel.button.setToolTipText("Add the student's grade for the course.");
			addButtonPanel.button.setEnabled(false);
			addButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						if(viewAddGrade(Short.parseShort(yearPanel.getSelectedItem()), semesterPanel.getSelectedItem(), 
								Integer.parseInt(courseIDPanel.getSelectedItem()), Integer.parseInt(studentIDPanel.getSelectedItem()), "Add", 
								(String) gradePanel.comboBox.getSelectedItem()) != null)
							JOptionPane.showMessageDialog(null,"Grade added.");
						else
							JOptionPane.showMessageDialog(null,"Grade has already been assigned");
					} catch (NumberFormatException | IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
				}
			});
			addToCenter(gradePanel, addButtonPanel);
		}
	}
	
	/*
	 * ViewGradesPanel functions similar to AddGradesPanel, except the user views an existing grade by choosing an enroll record and clicking
	 * viewButton, whose listener calls viewAddGrade(...). This time, viewAddGrade(...) returns null if a grade has not been added, and the grade read
	 * from the enroll file if a grade has been added.
	 * 		-If viewAddGrade(...) returns null, the user is informed that a grade has not yet been added.
	 * 		-otherwise, the grade is loaded into the grade JComboBox for the user to see.
	 */
	private class ViewGradesPanel extends GradesPanel {
		private static final long serialVersionUID = -8499921261606665631L;
		private ButtonPanel viewButton;
		private TextPanel gradePanel;
		public ViewGradesPanel() {
			super("View Grade", "View the grade of", "View Grade");
			yearPanel.comboBox.addActionListener(new YearListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					studentIDPanel.reload(new String[]{});
					studentIDPanel.comboBox.setEnabled(false);
					viewButton.button.setEnabled(false);
					gradePanel.textField.setText("  ");
					gradePanel.textField.setEnabled(false);
				}
			});
			semesterPanel.comboBox.addActionListener(new SemesterListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					studentIDPanel.reload(new String[]{});
					studentIDPanel.comboBox.setEnabled(false);
					viewButton.button.setEnabled(false);
					gradePanel.textField.setText("  ");
					gradePanel.textField.setEnabled(false);
				}
			});
			courseIDPanel.comboBox.addActionListener(new CourseIDListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					viewButton.button.setEnabled(false);
					gradePanel.textField.setText("  ");
					gradePanel.textField.setEnabled(false);
				}
			});
			studentIDPanel.comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewButton.button.setEnabled(true);
					gradePanel.textField.setText("  ");
					gradePanel.textField.setEnabled(false);
				}
			});
			viewButton = new ButtonPanel("View Grade (alt+V)");
			viewButton.button.setMnemonic(KeyEvent.VK_V);
			viewButton.button.setEnabled(false);
			viewButton.button.setToolTipText("View the student's grade for the course.");
			viewButton.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						String grade = viewAddGrade(Short.parseShort(yearPanel.getSelectedItem()), semesterPanel.getSelectedItem(), 
								Integer.parseInt(courseIDPanel.getSelectedItem()), Integer.parseInt(studentIDPanel.getSelectedItem()), "View", "");
						if(grade != null) 
							gradePanel.textField.setText(grade);
						else
							JOptionPane.showMessageDialog(null, "Grade has not yet been assigned.");
					} catch (NumberFormatException | IOException exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
					gradePanel.textField.setEnabled(true);
				}
			});
			gradePanel = new TextPanel("Grade",2);
			gradePanel.textField.setEnabled(false);
			addToCenter(viewButton, gradePanel);
		}
	}
	
	/*
	 * ReportPanel allows the user to search for all students taking a course in a given year and semester.
	 * Like the other Panels that inherit from GRPanel, the user must select year, semester, and courseID in order, with a selection
	 * enabling the next. 
	 * 		If no courses exist for a given year and semester, the user is informed by message dialog and the courseIDPanel is not enabled.
	 * 		otherwise, the user selects a course, clicks the searchButton, and a list of the students taking the course for the given
	 * 		year and semester are loaded into the JTable and displayed.
	 */
	private class ReportPanel extends GRPanel {
		private static final long serialVersionUID = -7073629582921359715L;
		private JScrollPane tablePane;
		private JTable table;
		private ButtonPanel searchButtonPanel;
		private String[] columnNames = {"Student ID","Name","Grade"};
		public ReportPanel() {
			super("Report of Enrolled Students", "View all of the students enrolled in a course for a given year and semester.", "Search");
			yearPanel.comboBox.addActionListener(new YearListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					searchButtonPanel.button.setEnabled(false);
					table.setModel(new DefaultTableModel(new String[][]{}, columnNames));
				}
			});
			semesterPanel.comboBox.addActionListener(new SemesterListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					super.actionPerformed(e);
					searchButtonPanel.button.setEnabled(false);
					table.setModel(new DefaultTableModel(new String[][]{}, columnNames));
				}
			});
			courseIDPanel.comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					searchButtonPanel.button.setEnabled(true);
					table.setModel(new DefaultTableModel(new String[][]{}, columnNames));
				}
			});
			searchButtonPanel = new ButtonPanel("Search (alt+S)");
			searchButtonPanel.button.setMnemonic(KeyEvent.VK_S);
			searchButtonPanel.button.setToolTipText("List all students taking the course for the given year and semester.");
			searchButtonPanel.button.setEnabled(false);
			searchButtonPanel.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						table.setModel(new DefaultTableModel(getReportDataFromEnrollFile(Short.parseShort(yearPanel.getSelectedItem()), semesterPanel.getSelectedItem(),
								Integer.parseInt((String) courseIDPanel.comboBox.getSelectedItem())), columnNames));
					} catch (NumberFormatException | IOException  exception) {
						JOptionPane.showMessageDialog(null, "Error: " + exception);
					}
				}
			});
			table = new JTable(new String[][]{}, columnNames);
			table.setRowHeight(40);
			table.getTableHeader().setPreferredSize(new Dimension(40,40));
			table.getTableHeader().setFont(new Font("Sans Serif", Font.PLAIN, 25));
			table.setFont(new Font("Sans Serif", Font.PLAIN, 20));
			tablePane = new JScrollPane(table);
			tablePane.setPreferredSize(new Dimension(400,200));
			table.setPreferredScrollableViewportSize(table.getPreferredSize());
			table.setFillsViewportHeight(true);
			addToCenter(searchButtonPanel);
			c.fill = GridBagConstraints.HORIZONTAL;
			addToCenter(tablePane);
		}
	}
	
	/*
	 * The following four panels provide a simple way to encapsulate a JComponent with a preset border, size, and/or dimension. 
	 * Being a panel also prevents the element from being fit to size and getting distorted/looking really ugly. 
	 */
	private class LabelPanel extends JPanel {
		private static final long serialVersionUID = -4826369702450694863L;
		private JLabel label;
		public LabelPanel(String text) {
			label = new JLabel(text);
			label.setFont(new Font("Dialog", Font.BOLD, 25));
			add(label);
		}
	}
	
	private class TextPanel extends JPanel {
		private static final long serialVersionUID = 6235603207216892144L;
		public JTextFieldLimit textField;
		public TextPanel(String type, int size) {
			setLayout(new GridLayout(1,1,15,0));
			setBorder(BorderFactory.createTitledBorder(null,type,TitledBorder.CENTER, TitledBorder.TOP,new Font("Monospaced",Font.BOLD,24)));
			textField = new JTextFieldLimit(size);
			textField.setPreferredSize(new Dimension(20,25));
			textField.setFont(new Font("Dialog", Font.PLAIN, 20));
			add(textField);
		}
	}
	
	private class ComboBoxPanel extends JPanel {
		private static final long serialVersionUID = -1148243685670326825L;
		private JComboBox<String> comboBox;
		public ComboBoxPanel(String type, String[] values) {
			setBorder(BorderFactory.createTitledBorder(null,type,TitledBorder.CENTER, TitledBorder.TOP,new Font("Monospaced",Font.BOLD,24)));
			comboBox = new JComboBox<String>(values);
			comboBox.setPreferredSize(new Dimension(200,30));
			comboBox.setFont(new Font("Dialog", Font.PLAIN, 20));
			comboBox.setMaximumRowCount(4);
			add(comboBox);
		}
		public String getSelectedItem() {
			return (String) comboBox.getSelectedItem();
		}
		public void reload(String[] newList) {
			comboBox.setModel(new DefaultComboBoxModel<String>(newList));
		}
	}
	
	private class ButtonPanel extends JPanel {
		private static final long serialVersionUID = -870340853032754291L;
		private JButton button;
		public ButtonPanel(String text) {
			button = new JButton(text);
			button.setPreferredSize(new Dimension(300,50));
			button.setFont(new Font("Dialog", Font.PLAIN, 20));
			add(button);
		}
	}
	
	/*
	 * writeSCRecord(...) writes a student/course record to its respective file (depending on the fileType passed to the method) and returns the ID generated.
	 * Student/course records are functionally identical except for the record they are written in.
	 * The ID is generated by taking the length of the respective student/course file, dividing by the length of a single student/course file,
	 * and adding the respective student or course offset.
	 */
	public int writeSCRecord(String[] textField, String fileType) throws IOException {
		if(fileType != "Student" && fileType != "Course") throw new IllegalArgumentException("Invalid file type.");
		RandomAccessFile file = mapRecordToStream.get(fileType);
		file.seek(file.length());
		int ID = (int) file.length() / SC_RECORD_SIZE + OFFSET.get(fileType);
		file.writeInt(ID);
		for(int index = 0; index < 2; index++) {
			file.writeChars(convertToProperLength(textField[index],TEXT_SIZE));
		}
		file.writeShort(Short.parseShort(textField[2]));
		file.writeChars(convertToProperLength(textField[3],TEXT_SIZE));
		return ID;
	}
	
	/*
	 * writeEnrollRecord(...) is described in EnrollPanel
	 */
	public boolean writeEnrollRecord(short year, String semester, int courseID, int studentID) throws IOException {
		RandomAccessFile file = mapRecordToStream.get("Enroll");
		short yearField;
		int	courseIDField,
			studentIDField;
		String semesterField;
		boolean recordDoesNotAlreadyExist = true;
		try {
			file.seek(0);
			while(true) {
				yearField = file.readShort();
				semesterField = readCharArray(file,6).trim();
				courseIDField = file.readInt();
				studentIDField = file.readInt();
				if(year == yearField && semester.equals(semesterField) && courseID == courseIDField && studentID == studentIDField) {
					recordDoesNotAlreadyExist = false;
				}
				file.seek(file.getFilePointer() + 4);
			} 	
		} catch(EOFException e) {}
		if(recordDoesNotAlreadyExist) {
			file.seek(file.length());
			file.writeShort(year);
			file.writeChars(convertToProperLength(semester,6));
			file.writeInt(courseID);
			file.writeInt(studentID);
			file.writeChars("IP");
		}
		return recordDoesNotAlreadyExist;
	}
	
	/*
	 * readTextFields(...) decodes the recordID passed to it into a byte position in the record, reversing the process that originally created the
	 * ID. 
	 * 		If the record does not exist (the recordID is either under or over the records in the file), null is returned.
	 * 		otherwise, the text fields are read from file and returned.
	 */
	public String[] readTextFields(int recordID, String fileType) throws IOException {
		if(fileType != "Student" && fileType != "Course") throw new IllegalArgumentException("Invalid file type.");
		String[] textFields = new String[4];
		try {
			RandomAccessFile file = mapRecordToStream.get(fileType);
			int bytePos = (recordID - OFFSET.get(fileType)) * SC_RECORD_SIZE + 4;
			if(bytePos < 0) return null; //If bytePos negative (under file) return null.
			file.seek(bytePos);
			for(int index = 0; index < 2; index++)
				textFields[index] = readCharArray(file,TEXT_SIZE).trim();
			textFields[2] = String.valueOf(file.readShort()).trim();
			textFields[3] = readCharArray(file,TEXT_SIZE).trim();
			return textFields;     //If the textFields are successfully read in, they are returned.
		} catch(EOFException e) {} //If bytePos is over file, EOFException is immediately thrown, and null is returned.
		
		return null;
	}
	
	/*
	 * The existing text fields in the record are overwritten by new text field input from a user.
	 * Note that viewUpdatePanel, the user of this method, already checked if the record existed with readTextFields,
	 * and doesn't enable the updateButton unless the record exists, so verification that the record exists is unnecessary.
	 */
	public void updateTextFields(int recordID, String[] textField, String fileType) throws IOException {
		if(fileType != "Student" && fileType != "Course") throw new IllegalArgumentException("Invalid file type.");
		try {
			RandomAccessFile file = mapRecordToStream.get(fileType);
			file.seek((recordID - OFFSET.get(fileType)) * SC_RECORD_SIZE + 4);
			for(int index = 0; index < 2; index++) {
				file.writeChars(convertToProperLength(textField[index],TEXT_SIZE));
			}
			file.writeShort(Short.parseShort(textField[2]));
			file.writeChars(convertToProperLength(textField[3],TEXT_SIZE));
		} catch(EOFException e) {}
	}
	
	public String[] getIDsFromSCFile(String fileType) throws IOException {
		if(fileType != "Student" && fileType != "Course") throw new IllegalArgumentException("Invalid file type.");
		List<String> IDList = new ArrayList<>();
		RandomAccessFile file = mapRecordToStream.get(fileType);
		file.seek(0);
		try {
			while(true) {
				IDList.add(String.valueOf(file.readInt()).trim());
				file.seek(file.getFilePointer() + (SC_RECORD_SIZE - 4));
			} 	
		} catch(EOFException e) {}
		
		return IDList.toArray(new String[IDList.size()]);
	}
	
	public String[] getCourseIDsFromEnrollFile(short year, String semester) throws IOException {
		Set<String> courseIDList = new TreeSet<>();
		short yearField;
		String semesterField;
		try {
			RandomAccessFile file = mapRecordToStream.get("Enroll");
			file.seek(0);
			while(true) {
				yearField = file.readShort();
				semesterField = readCharArray(file,6).trim();
				if(year == yearField && semester.equals(semesterField)) {
					courseIDList.add(String.valueOf(file.readInt()).trim());
					file.seek(file.getFilePointer() + 8);
				}
				else 
					file.seek(file.getFilePointer() + 12);
			} 	
		} catch(EOFException e) {}
		
		return courseIDList.toArray(new String[courseIDList.size()]);
	}
	
	public String[] getStudentIDsFromEnrollFile(short year, String semester, int courseID) throws IOException {
		List<String> studentIDList = new ArrayList<>();
		short yearField;
		int	courseIDField;
		String semesterField;
		try {
			RandomAccessFile file = mapRecordToStream.get("Enroll");
			file.seek(0);
			while(true) {
				yearField = file.readShort();
				semesterField = readCharArray(file,6).trim();
				courseIDField = file.readInt();
				if(year == yearField && semester.equals(semesterField) && courseID == courseIDField) {
					studentIDList.add(String.valueOf(file.readInt()).trim());
					file.seek(file.getFilePointer() + 4);
				}
				else 
					file.seek(file.getFilePointer() + 8);
			} 	
		} catch(EOFException e) {}
		
		return studentIDList.toArray(new String[studentIDList.size()]);
	}
	
	//Allows for viewing the grade of or writing a grade to an existing enrollment record, depending on the switch "action" argument passed to the function.
	public String viewAddGrade(short year, String semester, int courseID, int studentID, String action, String grade) throws IOException {
		if(action != "View" && action != "Add") throw new IllegalArgumentException("Invalid action.");
		RandomAccessFile file = mapRecordToStream.get("Enroll");
		short yearField;
		int	courseIDField,
			studentIDField;
		String semesterField;
		try {
			file.seek(0);
			while(true) {
				yearField = file.readShort();
				semesterField = readCharArray(file,6).trim();
				courseIDField = file.readInt();
				studentIDField = file.readInt();
				if(year == yearField && semester.equals(semesterField) && courseID == courseIDField && studentID == studentIDField) {
					if(action == "View") {
						if(!readCharArray(file,2).equals("IP")) {
							file.seek(file.getFilePointer() - 4);
							grade = readCharArray(file, 2).trim();
						}
						else 
							grade = null;
					}
					else {
						if(readCharArray(file,2).equals("IP")) {
							file.seek(file.getFilePointer() - 4);
							file.writeChars(convertToProperLength(grade,2));	
						}
						else 
							grade = null;
					}	
				}
				else
					file.seek(file.getFilePointer() + 4);
			} 	
		} catch(EOFException e) {}
		
		return grade;
	}
	
	public String[][] getReportDataFromEnrollFile(short year, String semester, int courseID) throws IOException {
		RandomAccessFile enrollFile = mapRecordToStream.get("Enroll");
		RandomAccessFile studentFile = mapRecordToStream.get("Student");
		enrollFile.seek(0);
		studentFile.seek(0);
		short yearField;
		int	courseIDField,
			studentIDField;
		String semesterField;
		List<String[]> reportData = new ArrayList<String[]>();
		try {
			while(true) {
				yearField = enrollFile.readShort();
				semesterField = readCharArray(enrollFile,6).trim();
				courseIDField = enrollFile.readInt();
				if(year == yearField && semester.equals(semesterField) && courseID == courseIDField) {
					String[] reportRow = new String[]{String.valueOf(studentIDField = enrollFile.readInt()),"",readCharArray(enrollFile,2).trim()};
					studentFile.seek((studentIDField - OFFSET.get("Student")) * SC_RECORD_SIZE + 4);
					reportRow[1] = readCharArray(studentFile,TEXT_SIZE).trim();
					reportData.add(reportRow);
				}
				else {
					enrollFile.seek(enrollFile.getFilePointer() + 8);
				}
			}
		} catch(EOFException e){}
		
		return reportData.toArray(new String[reportData.size()][3]);
	}
	
	private String readCharArray(RandomAccessFile file, int length) throws IOException {
		char[] buffer = new char[length];
		for(int index = 0; index < length; index++) 
			buffer[index] = file.readChar();
		return String.valueOf(buffer);
	}
	
	/*
	 * This method, convertToProperLength(..), is used to ensure that text strings stored in file are all the same length. 
	 */
	private String convertToProperLength(String text, int properLength) {
		char[] converter = new char[properLength];
		int textLength;
		textLength = text.length();
		for(int index = 0; index < textLength; index++)
			converter[index] = text.charAt(index);
		return String.valueOf(converter);
	}
	
	/*
	 * The JTextFieldLimit class was taken from here: https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
	 * This class is a subclass of JTextField that limits the maximum number of characters that can be typed into the text field. This ensures that
	 * input text does not exceed the fixed text length when writing to file.
	 */
	public class JTextFieldLimit extends JTextField {
		private static final long serialVersionUID = -5757799080643819444L;
		private int limit;

	    public JTextFieldLimit(int limit) {
	        super();
	        this.limit = limit;
	    }

	    @Override
	    protected Document createDefaultModel() {
	        return new LimitDocument();
	    }

	    private class LimitDocument extends PlainDocument {
			private static final long serialVersionUID = -4501963183965945655L;

			@Override
	        public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
	            if (str == null) return;

	            if ((getLength() + str.length()) <= limit) {
	                super.insertString(offset, str, attr);
	            }
	        }       

	    }

	}
	
	public static void main(String[] args) {
		new StudentInfoSystem();
	}
}