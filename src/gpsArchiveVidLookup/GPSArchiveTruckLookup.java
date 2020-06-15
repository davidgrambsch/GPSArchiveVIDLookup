package gpsArchiveVidLookup;
import java.sql.*;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


class GPSArchiveTruckLookup {
	
	public String inFileName;
	public String outFileName;
	public int processedLines; // number of lines read
	public static int delimiterSelected = 0;
	public static String[] delimiters = {"	",","};
	static String messageHeader = "\"Truck Number\",\"Date & Time\",\"G\",\"I\",\"Speed\",\"HD\",\"GPS Quality\",\"Location\",\"SS\",\"SID\",\"Odometer\",\"Lat\",\"Long\",\"Reason\",\"Info\",\"GPS Rolling Odometer\",\"ECM Odometer\",\"ECM Fuel\",\"ECM Speed\",\"ECM Idle Time\",\"Total PTO\",\"Driver 1\",\"Driver 1 Duty Status\",\"Driver 2\",\"Driver 2 Duty Status\",\"Trailer ID 1\",\"Trailer ID 2\",\"Fuel Tank 1 %\",\"Fuel Tank 2 %\",\"Ambient Temp\",\"PACOS ID\"";
	static HashMap<Integer, String> vehicleVidLookup;
	public static String staticTruckNumber;
	
	public GPSArchiveTruckLookup (String fileName) 
	{
		inFileName=fileName;
		vehicleVidLookup = new HashMap<Integer, String>();
	}
	public GPSArchiveTruckLookup (String fileName,String truckNumberString) 
	{
		inFileName=fileName;
		staticTruckNumber = truckNumberString;
		vehicleVidLookup = new HashMap<Integer, String>();
	}
	
	public int getDelimiter () 
	{
		return delimiterSelected;
	}
	public void setDelimiter (int newDelimiter) 
	{
			delimiterSelected = newDelimiter;
		return;
	}
	public String getOutfilename () 
	{
		return outFileName;
	}
	
	public String getStaticTruckNumber () 
	{
		return staticTruckNumber;
	}
	public void setStaticTruckNumber (String newStaticTruckNumber) 
	{
			staticTruckNumber = newStaticTruckNumber;
		return;
	}
	public void parseData()
	{
		BufferedReader inBuffer = null; //input buffer
		BufferedWriter outBuffer = null; //output buffer
		try {
			String rawText;
			String extensionString="";
			inBuffer = new BufferedReader(new FileReader(inFileName));
			if (inFileName.lastIndexOf(".") != -1)
			{
				extensionString = inFileName.substring(inFileName.lastIndexOf("."),inFileName.length());
			}
			outFileName = inFileName.replace(extensionString, "") + "_parsed" + ".csv";
			outBuffer = new BufferedWriter(new FileWriter(outFileName));
			ArrayList<String> fieldList = new ArrayList<String>();
			
			outBuffer.write(messageHeader + "\n");
			String outString = "";
			while ((rawText = inBuffer.readLine()) != null ) 
			{
				fieldList = rawTextToArrayList(rawText);
				if (!fieldList.isEmpty()) 
				{
					outString = parseGPS(fieldList);
					outBuffer.write(outString + "\n");
				}
				processedLines++;
			}
			outBuffer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				if (inBuffer != null) inBuffer.close();
			} 
			catch (IOException crunchifyException) 
			{
				crunchifyException.printStackTrace();
			}
		}
	}
	
	// convert CSV to ArrayList using Split
	public static ArrayList<String> rawTextToArrayList(String rawText) 
	{
		ArrayList<String> output = new ArrayList<String>();
		String otherThanQuote = " [^\"] ";
		String quotedString = String.format(" \" %s* \" ", otherThanQuote);
		String regex = String.format("(?x) "+ // enable comments, ignore white spaces
		",                         "+ // match delimiter
		"(?=                       "+ // start positive look ahead
		"  (                       "+ //   start group 1
		"    %s*                   "+ //     match 'otherThanQuote' zero or more times
		"    %s                    "+ //     match 'quotedString'
		"  )*                      "+ //   end group 1 and repeat it zero or more times
		"  %s*                     "+ //   match 'otherThanQuote'
		"  $                       "+ // match the end of the string
		")                         ", // stop positive look ahead
		otherThanQuote, quotedString, otherThanQuote);
		String[] splitData=null;
		if (rawText != null) 
		{
			if (delimiterSelected==1)
				splitData = rawText.split(regex, -1); 
			else if (delimiterSelected==0)
				splitData = rawText.split("	", -1); 
			else
				return null;
			for (int i = 0; i < splitData.length; i++) 
			{
				if (!(splitData[i] == null) && (splitData[i].length() > 1)) 
						if (i==0)
							if (splitData[0].substring(0,1).equals("\""))
								if(!splitData[0].substring(1,2).matches("[0-9]")) //ignore header row when " are used
									return output;
								else 
									output.add(splitData[i].trim().replace("\"", "")); //don't ignore " with a valid number
							else //" are not used
								if(!splitData[0].substring(0,1).matches("[0-9]")) //ignore header row
									return output;
								else 
									output.add(splitData[i].trim().replace("\"", ""));							
						else 
							output.add(splitData[i].trim().replace("\"", "")); //trim trailing/leading whitespace
			}
		}
		return output;
	}
		
	public static String parseGPS (ArrayList<String> fields) 
	{
		String outString="";
		try {
			int vid=Integer.valueOf(fields.get(0)); 
			String truckNumber;
			if (staticTruckNumber==null || staticTruckNumber=="" || staticTruckNumber.isEmpty())
				truckNumber = lookupVehicle(vid);
			else
				truckNumber = staticTruckNumber;
			
			outString = "\"" + truckNumber + "\"";
			for (int i = 1; i < fields.size() ;i++)  
				outString = outString + ",\"" + fields.get(i) + "\"";
		} 
		catch (Exception e)
		{
			System.out.println("exception in parseGPS: " + e.toString() );
			return outString;
		}
		return outString;
	}
	
	/* this code will do a lookup for vid
	* Ideally we would only look up each vid once and store the results for future lookups so we
	* don't have to query the DB each time
	*  */
	static String lookupVehicle (int vid)
	{
		String vehicle = vehicleVidLookup.get(vid);
		String deletedVehicle;
		String dsn;
		if (vehicle!=null) //if we have already looked up the vehicle, return the previously looked up value
		{
			//System.out.println("Local trucknum found for vid: " + vid + ", skipping lookup");
			return vehicle;
		}

		System.out.println("Looking up serv100 for vid: " + vid);
		vehicle = String.valueOf(vid);
		try 
		{
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Connection conn = DriverManager.getConnection("jdbc:sqlserver://serv100;databaseName=intouch_main;integratedSecurity=true");
			Statement stmt = conn.createStatement();
			ResultSet rs;
			rs = stmt.executeQuery("SELECT trucknum,deleted_trucknum,dsn FROM dbo.v_unit WHERE vid="+ vid);

			while ( rs.next() ) 
			{
				vehicle = rs.getString("trucknum");
				deletedVehicle = rs.getString("deleted_trucknum");
				dsn = rs.getString("dsn");
				System.out.println("Found Vehicle: " + vehicle + " deleted vehicle: " + deletedVehicle +" dsn: " + dsn);
				//check for data accuracy
				if(vehicle==null && deletedVehicle==null && dsn==null)// invalid trucknum, invalid deleted num, invalid dsn, vid is only choice to use
				{
					vehicle="VID: " + vid;
					System.out.println("No trucknum, deleted_trucknum, or dsn found for vid: " + vid + ", using " + vehicle + " for trucknum");
				}
				else if (vehicle==null && deletedVehicle==null ) // invalid trucknum, invalid deleted num, valid DSN
				{	
					vehicle="DSN: " + dsn;
					System.out.println("No trucknum or deleted_trucknum found for vid: " + vid + ", using " + vehicle + " for trucknum");
				}
				else if (vehicle==null) // invalid trucknum, valid deleted num
				{	
					vehicle=deletedVehicle + " (deleted)";
					System.out.println("No trucknum found for vid: " + vid + ", using " + vehicle + " for trucknum");
				}
				// valid trucknum
				System.out.println("Saving vid: " + vid + ", vehicle: " + vehicle + " to local data");
				vehicleVidLookup.put(vid, vehicle);
			}
			if (vehicle.equals(String.valueOf(vid)))
			{
				vehicle="VID: " + vid;
				System.out.println("no v_unit record found for vid: " + vid + ", using " + vehicle + " for trucknum");	
				System.out.println("Saving vid: " + vid + ", vehicle: " + vehicle + " to local data");
				vehicleVidLookup.put(vid, vehicle);
			}
			conn.close();
		} 
			catch (Exception e) 
		{
			System.out.println("Exception looking up VID: ");
			System.out.println(e.getMessage());
			System.out.println(e.getCause());
		}
		return vehicle;
	}
	
	public static String getCharacterDataFromElement(Element e) 
	{
		if (e == null)
			return " ";
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) 
		{
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return " ";
	}
		
	
	public static void main (String[] args) 
	{
		GPSArchiveTruckLookup myGPSLookup= new GPSArchiveTruckLookup("G:\\Team Drives\\L2 Data Services\\Data Requests (DR)\\DR01-DR99\\Completed\\DR64\\supporting files\\GPS_A74906923.csv");
		myGPSLookup.setDelimiter(1);
		myGPSLookup.parseData();
	}

}