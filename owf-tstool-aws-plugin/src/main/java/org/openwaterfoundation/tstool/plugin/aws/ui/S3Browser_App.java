// S3Browser_App - stand-alone application to browse and interact with S3 files

/* NoticeStart

OWF AWS TSTool Plugin
Copyright (C) 2022 Open Water Foundation

OWF TSTool AWS Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OWF TSTool AWS Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OWF TSTool AWS Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.aws.ui;

import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ProcessManager;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

/**
 * Application to provide an S3 file browser.
 * Open an application with JFrame.
 */
public class S3Browser_App implements Runnable {

	private static String title = "Aws - S3 Browser";
	
	private static String PROGRAM_NAME = "S3 Browser";
	private static String PROGRAM_VERSION = "1.0.0 (2023-01-16)";

	/**
	 * Create a browser application.
	 */
	public S3Browser_App ( JFrame parent, boolean modal, String title, AwsSession awsSession, String region ) {
		//this.title = title;
		//this.awsSession = awsSession;
		//this.region = region;

		boolean doSwing = true;

    	if ( doSwing ) {
    		// Run using swing utilities to better deal with threading.
    		//SwingUtilities.invokeAndWait(new Runnable() {
    		SwingUtilities.invokeLater(this);
    	}
	}
	
	/**
	 * Launch the S3 as a separate process.
	 * This allows running from TSTool.
	 */
	public static void launchBrowser ( String title, AwsSession awsSession, String region ) {
		String routine = S3Browser_App.class.getSimpleName() + ".launchBrowser";
		Message.printStatus(2, routine, "Launching S3 browser application." );
		// Put together the command line as a list:
		// - the JRE and classpath are the same as run for the calling program
		String quote = "";
		boolean useQuote = true;
		if ( useQuote ) {
			quote = "\"";
		}
		// Format the classpath that is used for TSTool:
		// - getting the classpath in the development environment results in a classpath that is > 8191 characters,
		//   which is the limit on Windows, so have to split apart and ignore parts that are not important
		StringBuilder classpath = new StringBuilder();
		List<String> classpathList = StringUtil.breakStringList(System.getProperty("java.class.path"), File.pathSeparator, 0);
		Message.printStatus(2,routine,"Application classpath has " + classpathList.size() + " parts.");
		for ( String path : classpathList ) {
			// Ignore files that are known to not be needed, in order to get under the 8191 character limit:
			// - exclude specific packages that are not used in the AWS browser application
			Message.printStatus(2,routine,"Checking path=" + path);
			if ( (path.indexOf(File.separator + "batik-") >= 0) || // Used for SVG.
				(path.indexOf(File.separator + "Blowfish") >= 0)  || // Used for encryption.
				(path.indexOf(File.separator + "apache-poi") >= 0) || // Used for Excel integration.
				(path.indexOf(File.separator + "JFreeChart") >= 0) // Used for graphing.
				) {
				Message.printStatus(2,routine,"  discarding");
				continue;
			}
			if ( classpath.length() > 0 ) {
				classpath.append(File.pathSeparator);
			}
			classpath.append(path);
		}
		// Get the classpath associated with plugins,
		// which are determined dynamically at runtime when plugins are loaded.
		// Launch the browser application depending on whether command parameters or a single command line:
		// - TODO smalers 2023-01-09 the single line does better with quotes
		List<String> pluginClasspathList = IOUtil.getApplicationPluginClasspath();
		StringBuilder pluginClasspath = new StringBuilder();
		if ( pluginClasspathList.size() > 0 ) {
			for ( String path : pluginClasspathList ) {
				pluginClasspath.append(File.pathSeparator);
				pluginClasspath.append(path);
			}
		}
		try {
			int timeoutMs = 120000;
			boolean useShell = true;
			ProcessManager pm = null;
			// The process should start immediately but for now use a timeout of 2 minutes.
			// Specify the command as a single string.
			StringBuilder commandString = new StringBuilder();
			if ( IOUtil.isUNIXMachine() ) {
				commandString.append( quote + System.getProperty("java.home") + File.separator + "bin"
					+ File.separator + "javaw" + quote);
			}
			else {
				commandString.append( quote + System.getProperty("java.home") + File.separator + "bin"
					+ File.separator + "javaw.exe" + quote);
			}
			commandString.append(" -classpath ");
			commandString.append(quote + classpath + pluginClasspath + quote);
			commandString.append(" org.openwaterfoundation.tstool.plugin.aws.ui.S3Browser_App");
			commandString.append(" --logfile ");
			// Add the first log folder that will work.
			for ( int i = 20; i >= 14; i-- ) {
				String logFile = System.getProperty("user.home") + File.separator + ".tstool" + File.separator
					+ i + File.separator + "logs" + File.separator + "S3Browser.log";
				File f = new File(logFile);
				if ( f.getParentFile().exists() ) {
					commandString.append(logFile);
				}
			}
			commandString.append(" --profile ");
			commandString.append(awsSession.getProfile());
			commandString.append(" --region ");
			commandString.append(region);
			// Turn on debug if debug is on in TSTool.
			if ( Message.isDebugOn ) {
				commandString.append(" --debug " + Message.getDebugLevel(Message.TERM_OUTPUT)
					+ "," + Message.getDebugLevel(Message.LOG_OUTPUT));
			}
			pm = new ProcessManager (
				commandString.toString(),
				timeoutMs,
	           	null, // Exit status indicator.
	           	useShell,
	           	new File(IOUtil.getProgramWorkingDir()));
			Thread t = new Thread ( pm );
            t.start();
		}
		catch ( Exception e ) {
			Message.printWarning(1, "", "Unable to run program (" + e + ")" );
		}
	}

	/**
	Open the log file.
	@param logFile log file to open
	*/
	private static void openLogFile ( String logFile ) {
		String routine = "S3Browser_App.openLogFile";
		
		File f = new File(logFile);
		if ( !f.getParentFile().exists() ) {
			Message.printWarning ( 1, routine, "Error opening log file \"" + logFile +
				"\" - log file parent folder does not exist.");
		}
		else {
			Message.printStatus ( 1, routine, "Opening log file: " + logFile );
			try {
				// Limit the log file to 50 MB.
				Message.setLogFileMaxSize(1024*1024*50);
               	Message.openLogFile ( logFile );
               	// Do it again so it goes into the log file.
               	Message.printStatus ( 1, routine, "Log file name: " + logFile );
			}
			catch (Exception e) {
				Message.printWarning ( 1, routine, "Error opening log file \"" + logFile + "\"");
			}
		}

		// Initialize message levels.
		Message.setDebugLevel ( Message.TERM_OUTPUT, 0 );
		Message.setDebugLevel ( Message.LOG_OUTPUT, 0 );
		Message.setStatusLevel ( Message.TERM_OUTPUT, 0 );
		Message.setStatusLevel ( Message.LOG_OUTPUT, 2 );
		Message.setWarningLevel ( Message.TERM_OUTPUT, 0 );
		Message.setWarningLevel ( Message.LOG_OUTPUT, 3 );

		// Indicate that message levels should be shown in messages, to allow
		// for a filter when displaying messages.

		Message.setPropValue ( "ShowMessageLevel=true" );
		Message.setPropValue ( "ShowMessageTag=true" );
	}

	/**
	Clean up and quit the program.
	@param status Program exit status.
	*/
	public static void quitProgram ( int status ) {
		String	routine = "S3Browser_App.quitProgram";
	
		Message.printStatus ( 1, routine, "Exiting with status " + status + "." );
	
		System.err.print( "STOP " + status + "\n" );
		Message.closeLogFile ();
		System.exit ( status );
	}

    public void run() {
    	// Launch the S3 browse dialog.
    	//new S3BrowseJDialog ( this.parent, this.modal, title, this.awsSession, this.region).response();
   	}

    /**
     * Run a stand-alone application to browse S3 files.
     * @param args command line arguments, currently allowed are --profile Profile and --region Region.
     */
    public static void main ( String args[] ) {
    	String routine = "S3Browser_App.main";

    	IOUtil.setProgramData ( PROGRAM_NAME, PROGRAM_VERSION, args );
	   	JGUIUtil.setAppNameForWindows("S3 Browser");

    	// Parse the command line to extract necessary data to browse S3.

    	String logFile = null;
    	String profile = "default";
    	String region = null;
    	int terminalDebugLevel = 0;
    	int logDebugLevel = 0;
    	
    	int iargMax = args.length - 1;
    	for ( int iarg = 0; iarg < args.length; iarg++ ) {
    		String arg = args[iarg];
    		if ( arg.equalsIgnoreCase("--debug") ) {
    			// --debug terminal,file    (levels for the terminal and the log file)
    			++iarg;
    			if ( iarg <= iargMax ) {
    				String [] parts = args[iarg].split(",");
    				if ( parts.length >= 2 ) {
    					try {
    						terminalDebugLevel = Integer.parseInt(parts[0].trim());
    						Message.setDebugLevel(Message.TERM_OUTPUT,terminalDebugLevel);
    					}
    					catch ( NumberFormatException e ) {
    						Message.printWarning(1, routine, "Debug level should be --debug terminal,logfile");
    					}
    					try {
    						logDebugLevel = Integer.parseInt(parts[1].trim());
    						Message.setDebugLevel(Message.LOG_OUTPUT,logDebugLevel);
    					}
    					catch ( NumberFormatException e ) {
    						Message.printWarning(1, routine, "Debug level should be --debug terminal,logfile");
    					}
    				}
    				else {
    					Message.printWarning(1, routine, "Debug level should be --debug terminal,logfile");
    				}
    			}
    		}
    		else if ( arg.equalsIgnoreCase("--logfile") ) {
    			// --logfile LogFile
    			++iarg;
    			if ( iarg <= iargMax ) {
    				logFile = args[iarg];
    			}
    		}
    		else if ( arg.equalsIgnoreCase("--profile") ) {
    			// --profile Profile
    			++iarg;
    			if ( iarg <= iargMax ) {
    				profile = args[iarg];
    			}
    		}
    		else if ( arg.equalsIgnoreCase("--region") ) {
    			// --region Region
    			++iarg;
    			if ( iarg <= iargMax ) {
    				region = args[iarg];
    			}
    		}
    	}
    	
    	// Open the log file:
    	// - TODO smalers 2023-01-09 this will be an issue if more than one S3 browser is run at the same time
    	if ( logFile == null ) {
    		// By default use a temporary file for the log file.
    		logFile = IOUtil.tempFileName() + "-S3Browser.log";
    	}
    	openLogFile ( logFile );
    	// Set the levels to make sure they are active after the log file has been opened.
    	Message.setDebugLevel(Message.TERM_OUTPUT,terminalDebugLevel);
    	Message.setDebugLevel(Message.LOG_OUTPUT,logDebugLevel);
   		Message.printStatus(1, routine, "Terminal debug level = " + Message.getDebugLevel(Message.TERM_OUTPUT));
   		Message.printStatus(1, routine, "Log file debug level = " + Message.getDebugLevel(Message.LOG_OUTPUT));
    	if ( (terminalDebugLevel > 0) || (logDebugLevel > 0) ) {
    		Message.setDebug(true);
    	}

    	// Create the JFrame for the browser.

    	try {
    		// Handle credentials.
	
    		AwsSession awsSession = new AwsSession(profile);
	   		ProfileCredentialsProvider credentialsProvider0 = null;
	   		credentialsProvider0 = ProfileCredentialsProvider.create(profile);
	   		ProfileCredentialsProvider credentialsProvider = credentialsProvider0;
	   		awsSession.setProfileCredentialsProvider(credentialsProvider);
	   		
	   		Message.printStatus(2, routine, "Starting the user interface.");
    		new S3Browser_JFrame ( S3Browser_App.title, awsSession, region );
    	}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error starting the S3 Browser." );
			Message.printWarning ( 1, routine, e );
			quitProgram ( 1 );
		}
    }

}