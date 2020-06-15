package gpsArchiveVidLookup;
// SimpleFileChooser.java
// A simple file chooser to see what it takes to make one of these work.
//
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;


public class SimpleFileChooser extends JFrame {

	GPSArchiveTruckLookup myGPSParser;
	public SimpleFileChooser() {
    super("Large GPS flat file VID to trucknum lookup");
    //setSize(600, 300);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
    
    Container c = getContentPane();
    c.setLayout(new GridLayout(4,1));

    JPanel jp4 = new JPanel();
    jp4.setLayout(new FlowLayout());
    JLabel staticTruck = new JLabel("<html>Truck Number:<br>(blank for serv100 lookup)</html>");
    jp4.add(staticTruck);
    JTextField staticTruckEntry = new JTextField(25);
    jp4.add(staticTruckEntry);
    c.add(jp4);
    
    JPanel jp2 = new JPanel();
    jp2.setLayout(new FlowLayout());
    c.add(jp2);
    
    
	JRadioButton[] delButtons = new JRadioButton[2];
    ButtonGroup bgIcons= new ButtonGroup();
    final String[] radioString = new String[] {"Tab Delimited","Comma Delimited"};
	
	final JLabel[] iconLabel= new JLabel[2];
	
	for (int i=0; i<2;i++) {
		iconLabel[i] = new JLabel(radioString[i]);
		jp2.add(iconLabel[i]);
        new JLabel(radioString[i]);  

        delButtons[i] = new JRadioButton("");
        delButtons[i].setMargin(new Insets(1,1,1,1));
    	bgIcons.add(delButtons[i]);
    	jp2.add(delButtons[i]);	
	}
	delButtons[0].setSelected(true);
	
    JButton openButton = new JButton("Open Raw GPS archive Data file");
    JButton saveButton = new JButton("Parse Raw File");

    JLabel statusbar = new JLabel("Filename: ");
    
    openButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        JFileChooser chooser = new JFileChooser();
        //FileNameExtensionFilter filter = new FileNameExtensionFilter(
          //      ".csv Files", "csv");
        //chooser.setFileFilter(filter);
        //chooser.setMultiSelectionEnabled(true);
        int option = chooser.showOpenDialog(SimpleFileChooser.this);
        if (option == JFileChooser.APPROVE_OPTION) {
          File sf = chooser.getSelectedFile();
          String filelist = "nothing";
          System.out.println(sf.toString());
          if (sf.canRead()) {
        	  filelist = sf.getAbsolutePath();
        	  myGPSParser = new GPSArchiveTruckLookup(sf.getAbsolutePath());
          }
          else
        	  filelist = "no read permissions for file";        		  
          
          statusbar.setText("<html>You chose: <br>" + filelist + "</html>");
        }
        else {
          statusbar.setText("You canceled.");
        }
      }
    });

    // Create a file chooser that opens up as a Save dialog
    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
    
    	  if (myGPSParser == null)

    		  statusbar.setText("No File Selected");
    	  else
    	  {
    		  statusbar.setText("<html><br>");
    		  	for(int k = 0; k < 2; k++){
    		  		if (delButtons[k].isSelected())
    		  			myGPSParser.setDelimiter(k);
    			}		  
    			  if (staticTruckEntry.getText()!=null && staticTruckEntry.getText()!="")
    				  myGPSParser.setStaticTruckNumber(staticTruckEntry.getText());
    			  myGPSParser.parseData();
        		  statusbar.setText(statusbar.getText() + myGPSParser.inFileName + " parsed" + "<br>");
    		  }
    		  statusbar.setText(statusbar.getText() + "</html>"); 
    	  }
      
    });

    JPanel jp = new JPanel();
    jp.setLayout(new FlowLayout());
    setSize(500, 200);
    c.add(jp);
    jp.add(openButton);
    jp.add(saveButton);
    JPanel jp3 = new JPanel();
    jp3.setLayout(new FlowLayout());
    c.add(jp3);
    jp3.add(statusbar);
    try {
		initAuthDLL();
	} catch (Exception e) {
		System.out.println("Error extracting sqljdbc_auth.dll");
	}
  }
   void initAuthDLL() throws URISyntaxException, IOException 
   {
		try {
		// gets sqljdbc_auth.dll from inside the JAR file as an input stream
		InputStream is = this.getClass().getResourceAsStream("/sqljdbc_auth.dll");
		//System.out.println("Null: " + (is == null));
		
		// sets the output stream to a system folder
		OutputStream os = new FileOutputStream("sqljdbc_auth.dll");
		
		// 2048 here is just my preference
		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
  public static void main(String args[]) {
	  System.out.println(" starting program ");
	  PrintStream out = null;
		try {
			out = new PrintStream(new FileOutputStream("log.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.setOut(out);
		  
	  SimpleFileChooser sfc = new SimpleFileChooser();
	  sfc.setVisible(true);
  }
}
  