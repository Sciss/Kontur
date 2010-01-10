/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.common.{ BasicApplication, BasicMenuFactory }
import de.sciss.gui.{ MenuAction, MenuGroup, MenuItem }
import de.sciss.kontur.{ Main }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.kontur.session.{ Session }
import java.awt.{ FileDialog, Frame }
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent }
import java.io.{ File }
import javax.swing.{ Action, KeyStroke }

class MenuFactory( app: BasicApplication )
extends BasicMenuFactory( app ) {
  import BasicMenuFactory._

   // ---- actions ----
   private val actionOpen = new ActionOpen( getResourceString( "menuOpen" ),
										    KeyStroke.getKeyStroke( KeyEvent.VK_O,
                                            MENU_SHORTCUT ))
	private val actionNewEmpty = new ActionNewEmpty( getResourceString( "menuNewEmpty" ),
											KeyStroke.getKeyStroke( KeyEvent.VK_N,
                                            MENU_SHORTCUT ))

    def openDocument( f: File ) {
		actionOpen.perform( f )
	}

	def showPreferences() {
		var prefsFrame = getApplication().getComponent( Main.COMP_PREFS ).asInstanceOf[ PrefsFrame ]

		if( prefsFrame == null ) {
			prefsFrame = new PrefsFrame()
		}
		prefsFrame.setVisible( true )
		prefsFrame.toFront()
	}

	protected def getOpenAction() : Action = actionOpen

  	protected def addMenuItems() {
		// --- file menu ---

		val mgFile      = get( "file" ).asInstanceOf[ MenuGroup ]
		val smgFileNew  = new MenuGroup( "new", getResourceString( "menuNew" ))
		smgFileNew.add( new MenuItem( "empty", actionNewEmpty ))
		mgFile.add( smgFileNew, 0 )

  		// --- timeline menu ---
		val mgTimeline  = new MenuGroup( "timeline", getResourceString( "menuTimeline" ))
		mgTimeline.add( new MenuItem( "trimToSelection", getResourceString( "menuTrimToSelection" ),
							  KeyStroke.getKeyStroke( KeyEvent.VK_F5, MENU_SHORTCUT )))

		mgTimeline.add( new MenuItem( "insertSpan", getResourceString( "menuInsertSpan" ),
							  KeyStroke.getKeyStroke( KeyEvent.VK_E, MENU_SHORTCUT + InputEvent.SHIFT_MASK )))
		add( mgTimeline, indexOf( "edit" ) + 1 )
    }

  // ---- internal classes ----
	// action for the New-Empty Document menu item
	protected class ActionNewEmpty( text: String, shortcut: KeyStroke )
	extends MenuAction( text, shortcut )
	{
		def actionPerformed( e: ActionEvent ) {
//			final AudioFileDescr afd = query();
//			if( afd != null ) {
              perform // ( afd )
//            }
		}

		protected def perform: Session = {
//			try {
				val doc = Session.newEmpty // ( afd );
				AbstractApplication.getApplication().getDocumentHandler().addDocument( this, doc );
				new SessionFrame( doc )
				doc
//			}
//			catch( IOException e1 ) {	// should never happen
//				BasicWindowHandler.showErrorDialog( null, e1, getValue( Action.NAME ).toString() );
//				return null;
//			}
		}
	}

	protected class ActionOpen( text: String, shortcut: KeyStroke )
	extends MenuAction( text, shortcut )
	{
		/*
		 *  Open a Session. If the current Session
		 *  contains unsaved changes, the user is prompted
		 *  to confirm. A file chooser will pop up for
		 *  the user to select the session to open.
		 */
		def actionPerformed( e: ActionEvent ) {
			queryFile().foreach( f => perform( f ))
		}

        private def queryFile() : Option[ File ] = {
			val w = getApplication().getComponent( Main.COMP_MAIN ).asInstanceOf[ AbstractWindow ]
			val frame	= w.getWindow() match {
               case f: Frame => f
               case _ => null
            }
			val prefs = getApplication().getUserPrefs()

			val fDlg = new FileDialog( frame, getResourceString( "fileDlgOpen" ), FileDialog.LOAD )
			fDlg.setDirectory( prefs.get( PrefsUtil.KEY_FILEOPENDIR, System.getProperty( "user.home" )))
			fDlg.setVisible( true )
			val strDir	= fDlg.getDirectory()
			val strFile	= fDlg.getFile()

			if( strFile == null ) return None;   // means the dialog was cancelled

			// save dir prefs
			prefs.put( PrefsUtil.KEY_FILEOPENDIR, strDir )

			Some( new File( strDir, strFile ))
		}

        /**
     	 *  Loads a new document file.
		 *  a <code>ProcessingThread</code>
		 *  started which loads the new session.
		 *
		 *  @param  path	the file of the document to be loaded
		 *
		 *  @synchronization	this method must be called in event thread
		 */
		def perform( path: File ) {
/*			Session	doc;

			// check if the document is already open
			doc = findDocumentForPath( path );
			if( doc != null ) {
				doc.getFrame().setVisible( true );
				doc.getFrame().toFront();
				return;
			}

			try {
				doc		= Session.newFrom( path );
				addRecent( doc.getDisplayDescr().file );
				AbstractApplication.getApplication().getDocumentHandler().addDocument( this, doc );
				doc.createFrame();	// must be performed after the doc was added
			}
			catch( IOException e1 ) {
				BasicWindowHandler.showErrorDialog( null, e1, getValue( Action.NAME ).toString() );
			}
*/
		}
	}
}
