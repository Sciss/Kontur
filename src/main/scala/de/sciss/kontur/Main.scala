/*
 *  Main.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.kontur

import java.awt.{ EventQueue }
import javax.swing.{ UIManager }
import de.sciss.app.{ DocumentHandler }
import de.sciss.common.{ AppWindow, BasicApplication, BasicDocument, BasicMenuFactory,
                         BasicWindowHandler, ProcessingThread }
import de.sciss.gui.{ GUIUtil }
import de.sciss.kontur.gui.{ MainFrame, MenuFactory, SuperColliderFrame }
import de.sciss.kontur.sc.{ SuperColliderClient }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.util.{ Flag }

/**
 *  The <code>Main</code> class contains the java VM
 *  startup static <code>main</code> method which
 *  creates a new instance of <code>Main</code>. This instance
 *  will initialize localized strings (ResourceBundle),
 *  Preferences, the <code>transport</code>, the <code>menuFactory</code>
 *  object (a prototype of the applications menu and its
 *  actions).
 *  <p>
 *  Common components are created and registered:
 *  <code>SuperColliderFrame</code>, <code>TransportPalette</code>,
 *  <code>ObserverPalette</code>, and <code>DocumentFrame</code>.
 *  <p>
 *  The <code>Main</code> class extends the <code>Application</code>
 *  class from the <code>de.sciss.app</code> package.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.11, 09-May-10
 *
 *	@todo		OSC /main/quit doesn't work repeatedly
 *				; seems to be a problem of menuFactory.closeAll!
 */

object Main {
    private val APP_NAME	= "Kontur"

	/*
	 *  Current version of the application. This is stored
	 *  in the preferences file.
	 *
	 *  @todo   should be saved in the session file as well
	 */
	private val APP_VERSION		= 0.11

	/**
	 *  Enables / disables event dispatching debugging
	 */
//	public static final boolean DEBUG_EVENTS	= false;

	/*
	 *  The MacOS file creator string.
	 */
	private val CREATOR			= "Ttm "

	/**
	 *  Value for add/getComponent(): the preferences frame
	 *
	 *  @see	#getComponent( Object )
	 */
	val COMP_PREFS		= "Prefs"
	/**
	 *  Value for add/getComponent(): the observer palette
	 *
	 *  @see	#getComponent( Object )
	 */
	val COMP_OBSERVER	= "Observer"
	/**
	 *  Value for add/getComponent(): the main log frame
	 *
	 *  @see	#getComponent( Object )
	 */
	val COMP_MAIN		= "Main"
	/**
	 *  Value for add/getComponent(): the online help display frame
	 *
	 *  @see	#getComponent( Object )
	 */
	val COMP_HELP  		= "Help"

    /**
	 *  java VM starting method. does some
	 *  static initializations and then creates
	 *  an instance of <code>Main</code>.
	 *
	 *  @param  args	are not parsed.
	 */
	def main( args: Array[ String ]) {
		// --- run the main application ---
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		EventQueue.invokeLater( new Runnable() {
			def run() {
				new Main( args )
			}
		})
	}
}

class Main( args: Array[ String ])
extends BasicApplication( classOf[ Main ], Main.APP_NAME ) {
   	private val quitAfterSaveListener = new ProcessingThread.Listener {
		def processStarted( e: ProcessingThread.Event ) { /* empty */ }

        // if the saving was successfull, we will call closeAll again
		def processStopped( e: ProcessingThread.Event ) {
			if( e.isDone ) quit
		}
	}

    // ---- constructor ----
    preInit

	/**
	 *	The arguments may contain the following options:
	 *	<UL>
	 *	<LI>-laf &lt;screenName&gt; &lt;className&gt; : set the default look-and-feel</LI>
	 *	</UL>
	 *
	 *	All other arguments not starting with a hyphen are considered to be paths to documents
	 *	that will be opened after launch.
	 */
	private def preInit {
		val prefs = getUserPrefs()

		// ---- init prefs ----

//        val prefsVersion = prefs.getDouble( PrefsUtil.KEY_VERSION, 0.0 )
//		if( prefsVersion < APP_VERSION ) {
//			warnings = PrefsUtil.createDefaults( prefs, prefsVersion )
//		} else {
//			warnings = null
//		}

		// ---- check commandline options ----

		var lafName = prefs.get( PrefsUtil.KEY_LOOKANDFEEL, null )
        var openDoc = scala.collection.immutable.Queue[ String ]()
        var i = 0
        while( i < args.length ) {
			if( args( i ).startsWith( "-" )) {
				if( args( i ).equals( "-laf" )) {
					if( (i + 2) < args.length ) {
						UIManager.installLookAndFeel( args( i + 1 ), args( i + 2 ))
						if( lafName == null ) lafName = args( i + 2 )
						i += 2
					} else {
						System.err.println( "Option -laf requires two additional arguments (screen-name and class-name)." )
						System.exit( 1 )
					}
				} else {
					System.err.println( "Unknown option " + args( i ))
					System.exit( 1 )
				}
			} else {
				openDoc = openDoc.enqueue( args( i ))
			}
            i += 1
		}

		// ---- init look-and-feel ----

		System.setProperty( "swing.aatext", "true" )
//		lookAndFeelUpdate( lafName )

		// ---- init infrastructure ----
		// warning : reihenfolge is crucial
//		val superCollider = SuperColliderClient.instance

		init()

		// ---- listeners ----

//		try {
//			superCollider.init();
//		}
//		catch( IOException e1 ) {
//			BasicWindowHandler.showErrorDialog( null, e1, "SuperColliderClient Initialization" );
//			System.exit( 1 );
//			return;
//		}

		// ---- component views ----

		val mainFrame	= new MainFrame()
		getWindowHandler().asInstanceOf[ BasicWindowHandler ].setDefaultBorrower( mainFrame )
//        val ctrlRoom	= new ControlRoomFrame()
//		val observer	= new ObserverPalette()
		val scFrame = new SuperColliderFrame()
        scFrame.setVisible( true )

		// means no preferences found, so
		// do some more default initializations
		// and display splash screen
//		if( prefsVersion == 0.0 ) {
//			ctrlRoom.setVisible( true )
//			observer.setVisible( true )
//			if( cache.getFolder().isDirectory() ) {
//				cache.setActive( true );
//			}
//  		new WelcomeScreen( this );
//		}

//		if( warnings != null ) {
//			for( int i = 0; i < warnings.size(); i++ ) {
//				System.err.println( warnings.get( i ));
//			}
//		}

//		if( prefs.node( PrefsUtil.NODE_AUDIO ).getBoolean( PrefsUtil.KEY_AUTOBOOT, false )) {
//			superCollider.boot();
//		}

//		if( openDoc != null ) {
//			for( int i = 0; i < openDoc.size(); i++ ) {
//				getMenuFactory().openDocument( new File( openDoc.get( i ).toString() ));
//			}
//		}
	}

	protected def createMenuFactory() : BasicMenuFactory = new MenuFactory( this )
	protected def createDocumentHandler() : DocumentHandler = new de.sciss.kontur.session.DocumentHandler( this )
	protected def createWindowHandler() : BasicWindowHandler = new BasicWindowHandler( this )

	private var forcedQuit = false

    override def quit() {
//      this.synchronized {
          val confirmed = new Flag( false )
          val pt          = getMenuFactory().closeAll( forcedQuit, confirmed )

//println( "---1" )
          if( pt != null ) {
//println( "---2" )
            pt.addListener( quitAfterSaveListener )
            pt.getClientArg( "doc" ).asInstanceOf[ BasicDocument ].start( pt )
          } else if( confirmed.isSet() ) {
//println( "---3" )
//			OSCRoot.getInstance().quit();
//			SuperColliderClient.getInstance().quit();
			super.quit()
		  }
//      }
	}

    def forceQuit() {
		forcedQuit = true
		quit()
	}

    private def lookAndFeelUpdate( className: String ) {
        if( className != null ) {
            try {
                UIManager.setLookAndFeel( className )
				AppWindow.lookAndFeelUpdate()
            }
            catch { case e1: Exception => GUIUtil.displayError( null, e1, null )}
        }
    }

// ------------ Application interface ------------

	def getMacOSCreator() : String = Main.CREATOR
	def getVersion(): Double = Main.APP_VERSION
}