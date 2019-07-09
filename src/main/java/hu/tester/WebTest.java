package hu.tester;
 
import java.io.BufferedReader;
import java.io.File;
import org.apache.commons.io.FileUtils; 
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;

import com.google.common.base.Splitter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WebTest implements Runnable {

	final static Logger logger = Logger.getLogger(WebTest.class);

	
	private String  WEB_BROWSER;
	private Integer THREAD_NUMBER = 1;
	private String  RUN_MODE;
	private String  SEPARATOR;
	private String  ROOT_FOLDER;
	private String  FILES_FIRST_TAG;
	private String  LOGFILE_PATH;
	private String  IMGFILE_PATH;
	private String  DATAFILE_NAME;

	private String  MAIL_LEVEL;
	private String  FROM_ADDRESS;
	private String  TO_ADDRESS;
	private String  SMTP_USERNAME;
	private String  SMTP_PASSWORD;
	private String  SMTP_SERVER;
	private String  SMTP_PORT;
	
	private String  EXCEL_FILE_NAME;
	
	private InputStream dataFileName;
	private BufferedReader dataFileReader;
	
	private String threadDir;
	private String logDir;
	private String imgDir;
	
	private WebDriver webDriver;
	
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	private Date today = new Date();
	
	private XSSFWorkbook workbook;
	private XSSFSheet sheet;
	private int excelRowNum = 5;
	
	private FileWriter logFile;
	
	//beolvasom a property adatait, mert kell, hogy mennyi szálat indítsunk
	public static void main(String[] args) throws IOException {
		WebTest obj = new WebTest();
		obj.readThread();
		obj.process();
	}
	
	//A szálak indítása után fut. Beolvasom az osztály részére a properties értékeit és elindul
	//a leíró file feldolgozása
	public void run() {
		Thread t = Thread.currentThread().currentThread();
		//paraméterek felolvasása: config.properties
		readProperties();
		//webDriver inicializálása
		setWebdriver();
		createFolder(t.getName());
		logInitialize();
		excelInitialize();
		try {
			readDataFileLines(t.getName());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			logClose();
			excelClose();
		}
    }

	//Ahány szálat kell indítani paraméter szerint, annyi indítása
	private void process()  throws IOException {
		for (int i=1; i <= THREAD_NUMBER; i++) {
			(new Thread(new WebTest())).start();
		}
	}
	
	//A leírófile megnyitása, a sorok felolvasása
	private void readDataFileLines(String t) throws IOException {
		dataFileName = Thread.currentThread().getContextClassLoader().getResourceAsStream(DATAFILE_NAME);
		BufferedReader dataFileReader = new BufferedReader(new InputStreamReader(dataFileName, "cp1250"));
		while(dataFileReader.ready()) {
		     String line = dataFileReader.readLine();
		     lineProcess(t, line); 
		}
		dataFileReader.close();

	}
	
	private void lineProcess(String t, String line) {
		//a commands-be kerül az aktuális sor tételei
		List<String> commands = splitter(line, SEPARATOR);
		insertExcelLine(commands);
		screenCapture("sorszam");
		insertLogLine(line);
		
		
	}
		
	
	
	
	
	
	
	private List<String> splitter(String line, String separator) {
		List<String> retVal = Splitter.on(separator).splitToList(line);
		return retVal;
	}

//************************LOG
	private void logInitialize() {
		try {
			logFile = new FileWriter(threadDir + "/" + LOGFILE_PATH + "/" + FILES_FIRST_TAG + ".txt");
		} catch (IOException e) {
			logger.error("Error write logfile " + e);
		}
	}
	
	private void insertLogLine(String logLine) {
		try {
			logFile.write(logLine + "\n");
		} catch (IOException e) {
			logger.error("Error write to logfile: " +logLine + " " + e);
		}
	}
	
	private void logClose() {
		try {
			logFile.close();
		} catch (IOException e) {
			logger.error("Error close logfile: " + e);
		}
	}
	
//************************EXCEL	
	private void excelInitialize() {
		EXCEL_FILE_NAME = threadDir + "/" + FILES_FIRST_TAG + ".xlsx";
		workbook = new XSSFWorkbook();
		sheet = workbook.createSheet(FILES_FIRST_TAG);
		
	}
	
	private void insertExcelLine(List<String> logLine) {
		Row row = sheet.createRow(excelRowNum);
		//Ha a logLine sor mérete 1, akkor comment és ezt jelöljük 
		for (int i=0; i<logLine.size(); i++) {
			if (logLine.size() == 1) {
				Cell cell = row.createCell(i + 2);
				cell.setCellValue("comment");
			}
			Cell cell = row.createCell(i + 3);
			cell.setCellValue(logLine.get(i));
		}			
		excelRowNum++;
	}
	
	private void excelClose() {
        try {
            FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE_NAME);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
        	logger.error("Error in excel file created: ", e);
        } catch (IOException e) {
        	logger.error("Error in excel file created: ", e);        	
        }		
	}
	
//*************************SCREEN CAPTURE
	private void screenCapture(String fileAzon) {
		try {
			System.out.println(webDriver);
			File scrFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
			FileUtils.copyFile(scrFile, new File(threadDir + "/" + IMGFILE_PATH + "/" + FILES_FIRST_TAG + "_" + fileAzon + ".png"));
		} catch (IOException e) {
			logger.error("IOException in screen capture " + e);			
		}


		
	}
	
	
//*************************SETTERS
	private void createFolder(String t) {
		String stringDate = dateFormat.format(today);
		File theDir = new File(ROOT_FOLDER + "/" + FILES_FIRST_TAG + "_" + t + "_" + stringDate);
		logger.info("Start create directory: " + theDir.getPath());
		if (!theDir.exists()) {
			try{
		        theDir.mkdir();
		        threadDir = theDir.getPath();
		        File theDirLog = new File(threadDir + "/" + LOGFILE_PATH);
		        theDirLog.mkdir();
		        logDir = theDirLog.getPath();
		        File theDirImg = new File(threadDir + "/" + IMGFILE_PATH);
		        theDirImg.mkdir();
		        imgDir = theDirImg.getPath();
		        logger.info("Created thread_dir=" + threadDir + " folder");
		    } catch(SecurityException se){
		        logger.error("Error in created " + theDir + " folder ()", se);
		    } catch(Exception e){
		        logger.error("Error in created " + theDir + " folder ()", e);
		    }        
			
		}
	}
	
	private void setWebdriver() {
		if (WEB_BROWSER.equals("Firefox")) { webDriver = new FirefoxDriver(); }
		if (WEB_BROWSER.equals("Chrome"))  { webDriver = new ChromeDriver();  }
		if (WEB_BROWSER.equals("Safari"))  { webDriver = new SafariDriver();  }
		if (WEB_BROWSER.equals("Firefox --headless")) {
			FirefoxOptions options = new FirefoxOptions()
					.addArguments("--headless");
			webDriver = new FirefoxDriver(options);
		}
		if (WEB_BROWSER.equals("Chrome --headless")) {
			ChromeOptions options = new ChromeOptions()
					.addArguments("headless");
			webDriver = new ChromeDriver(options);
		}
		logger.info("Selected broser: " + WEB_BROWSER + " selected driver: " + webDriver);
	}
	
	private void readProperties() {
		Properties prop = new Properties();
		InputStream input = null;
		try {			
			input = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
			prop.load(input);
			WEB_BROWSER     = prop.getProperty("browser");		//Firefox, Chrome, Edge, Safari, Firefox --headless, Chrome --headless
			String thread   = prop.getProperty("threads");	
			THREAD_NUMBER   = Integer.parseInt(thread);  		//Szálak száma
			RUN_MODE        = prop.getProperty("run_mode");		//Futás típusa: Check, csak ellenõrzés, nem kell excel és log, csak ha baj van, Test
			SEPARATOR       = prop.getProperty("separator");	//A leírófile szeparátora
			ROOT_FOLDER     = prop.getProperty("root_folder");	//Ahova a file-okat tartalmazó könyvtár kerüljön
			FILES_FIRST_TAG = prop.getProperty("files_first_tag");	//A file-ok nevének elsõ tagja
			LOGFILE_PATH    = prop.getProperty("logfile_path");	//A logfile helye
			IMGFILE_PATH    = prop.getProperty("photo_path");	//A képek helye
			DATAFILE_NAME   = prop.getProperty("data_file_name");	//A leírófile neve. 
			MAIL_LEVEL      = prop.getProperty("mail_send");
			FROM_ADDRESS    = prop.getProperty("fromAddress");
			TO_ADDRESS      = prop.getProperty("toAddress");
			SMTP_USERNAME   = prop.getProperty("username");
			SMTP_PASSWORD   = prop.getProperty("password");
			SMTP_SERVER     = prop.getProperty("server");
			SMTP_PORT       = prop.getProperty("port");
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//Elsõre elég csak a szálak számát beolvasni, ezért itt csak azt tesszük meg
	private void readThread() {
		Properties prop = new Properties();
		InputStream input = null;
		try {			
			input = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
			prop.load(input);
			String thread = prop.getProperty("threads");
			THREAD_NUMBER = Integer.parseInt(thread);  
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}