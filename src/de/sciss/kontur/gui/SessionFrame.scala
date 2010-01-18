/*
 *  SessionFrame.scala
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

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow, DynamicAncestorAdapter }
import de.sciss.common.{ BasicApplication, BasicWindowHandler, ShowWindowAction }
import de.sciss.gui.{ GUIUtil, MenuAction }
import de.sciss.util.{ Flag }
import de.sciss.kontur.session.{ Session }
import java.awt.{ BorderLayout, FileDialog, Frame }
import java.awt.event.{ ActionEvent, MouseAdapter, MouseEvent }
import java.io.{ File, IOException }
import javax.swing.{ Action, DropMode, JScrollPane, JTree, ScrollPaneConstants }
import javax.swing.tree.{ TreeNode }

class SessionFrame( doc: Session )
extends AppWindow( AbstractWindow.REGULAR ) {

  	private var writeProtected	= false
	private var wpHaveWarned	= false
    private val actionShowWindow= new ShowWindowAction( this )

    private val actionClose     = new ActionClose()
    private val actionSave      = new ActionSave()
    private val actionSaveAs	= new ActionSaveAs( false )

    // ---- constructor ----
    {
		// ---- menus and actions ----
		val mr = app.getMenuBarRoot
		mr.putMimic( "file.close", this, actionClose )
		mr.putMimic( "file.save", this, actionSave )
		mr.putMimic( "file.saveAs", this, actionSaveAs )

      val cp = getContentPane
      val sessionTreeModel = new SessionTreeModel( doc )
      val ggTree = new JTree( sessionTreeModel )
      ggTree.setDropMode( DropMode.INSERT )
      ggTree.setRootVisible( true )
      ggTree.setShowsRootHandles( true )
      val ggScroll = new JScrollPane( ggTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		                         	   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )

      ggTree.addMouseListener( new MouseAdapter() {
       override def mousePressed( e: MouseEvent ) {
          val selRow  = ggTree.getRowForLocation( e.getX(), e.getY() )
          if( selRow == -1 ) return
          val selPath = ggTree.getPathForLocation( e.getX(), e.getY() )
          val node = selPath.getLastPathComponent()

          if( e.isPopupTrigger ) popup( node, e )
          else if( e.getClickCount == 2 ) doubleClick( node, e )
       }
       
        private def popup( node: AnyRef, e: MouseEvent ): Unit = node match {
           case hcm: HasContextMenu => {
               hcm.createContextMenu.foreach( root => {
                   val pop = root.createPopup( SessionFrame.this )
                   pop.show( e.getComponent(), e.getX(), e.getY() )
               })
           }
             case _ =>
         }

        private def doubleClick( node: AnyRef, e: MouseEvent ): Unit = node match {
           case hdca: HasDoubleClickAction => hdca.doubleClickAction
             case _ =>
         }
      })

      cp.add( ggScroll, BorderLayout.CENTER )
      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!

      addDynamicListening( sessionTreeModel )

      val winListener = new AbstractWindow.Adapter() {
          override def windowClosing( e: AbstractWindow.Event ) {
              actionClose.perform
          }

          override def windowActivated( e: AbstractWindow.Event ) {
              // need to check 'disposed' to avoid runtime exception in doc handler if document was just closed
//              if( !disposed ) {
                  app.getDocumentHandler().setActiveDocument( SessionFrame.this, doc )
                  app.getWindowHandler().asInstanceOf[ BasicWindowHandler ].setMenuBarBorrower( SessionFrame.this )
//              }
          }
      }
      addListener( winListener )

      init()
  	  updateTitle
      doc.addListener( _ match {
          case Session.DirtyChanged( _ ) => updateTitle
          case Session.PathChanged( _, _ ) => updateTitle
      })

//      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

	/**
	 *  Recreates the main frame's title bar
	 *  after a sessions name changed (clear/load/save as session)
	 */
	def updateTitle {
		writeProtected	= false

//		actionRevealFile.setFile( afds.length == 0 ? null : afds[ 0 ].file );

		val name = doc.displayName
//			try {
//				for( int i = 0; i < afds.length; i++ ) {
//					f = afds[ i ].file;
//					if( f == null ) continue;
//					writeProtected |= !f.canWrite() || ((f.getParentFile() != null) && !f.getParentFile().canWrite());
//				}
//			} catch( SecurityException e ) { /* ignored */ }

//		if( writeProtected ) {
//			val icn = GUIUtil.getNoWriteIcon()
//			if( lbWriteProtected.getIcon() != icn ) {
//				lbWriteProtected.setIcon( icn )
//
//		} else if( lbWriteProtected.getIcon() != null ) {
//			lbWriteProtected.setIcon( null );
//		}

		setTitle( (if( !internalFrames ) app.getName() else "") +
                   (if( doc.isDirty() ) " - \u2022" else " - ") + name )

        actionShowWindow.putValue( Action.NAME, name )
//		actionSave.setEnabled( !writeProtected && doc.isDirty() )
		setDirty( doc.isDirty() )
        setWindowFile( doc.path getOrElse null )

//		final AudioFileInfoPalette infoBox = (AudioFileInfoPalette) app.getComponent( Main.COMP_AUDIOINFO )
//		if( infoBox != null ) infoBox.updateDocumentName( doc )

//		if( writeProtected && !wpHaveWarned && doc.isDirty() ) {
//			final JOptionPane op = new JOptionPane( getResourceString( "warnWriteProtected" ), JOptionPane.WARNING_MESSAGE )
//			BasicWindowHandler.showDialog( op, getWindow(), getResourceString( "msgDlgWarn" ))
//			wpHaveWarned = true
//		}
	}

	private class ActionClose extends MenuAction {
		def actionPerformed( e: ActionEvent ): Unit = perform

        def perform {
            doc.closeDocument( false, new Flag( false )) // XXX confirm unsaved
		}
	}

	// action for the Save-Session menu item
	private class ActionSave
	extends MenuAction {
		/**
		 *  Saves a Session. If the file
		 *  wasn't saved before, a file chooser
		 *  is shown before.
		 */
		def actionPerformed( e: ActionEvent ) {
          val name = getValue( Action.NAME ).toString
           (doc.path orElse actionSaveAs.query( name )).foreach( f =>
              perform( name, f, false, false ))
		}

		protected[gui] def perform( name: String, file: File, asCopy: Boolean, openAfterSave: Boolean ) {
            try {
               doc.save( file )
//                wpHaveWarned = false

                if( !asCopy ) {
                    app.getMenuFactory().addRecent( file )
                }
                if( openAfterSave ) {
                    app.getMenuFactory().openDocument( file )
                }
            }
            catch { case e1: IOException =>
				BasicWindowHandler.showErrorDialog( getWindow(), e1, name )
            }
		}
	}

	// action for the Save-Session-As menu item
	private class ActionSaveAs( asCopy: Boolean )
	extends MenuAction
	{
		private val openAfterSave = new Flag( false )

		/*
		 *  Query a file name from the user and save the Session
		 */
		def actionPerformed( e: ActionEvent ) {
            val name = getValue( Action.NAME ).toString
            query( name ).foreach( f => {
				actionSave.perform( name, f, asCopy, openAfterSave.isSet )
            })
		}

		/**
		 *  Open a file chooser so the user
		 *  can select a new output file and format for the session.
		 *
		 *  @return the AudioFileDescr representing the chosen file path
		 *			and format or <code>null</code>
		 *			if the dialog was cancelled.
		 *
		 *	@todo	should warn user if saveMarkers is true and format does not support it
		 */
		protected[gui] def query( name: String ) : Option[ File ] = {
          val dlg = new FileDialog( null.asInstanceOf[ Frame ], name, FileDialog.SAVE )
//          dlg.setFilenameFilter( this )
//          dlg.show
          BasicWindowHandler.showDialog( dlg )
          val dirName   = dlg.getDirectory
          val fileName  = dlg.getFile
          if( dirName != null && fileName != null ) {
            Some( new File( dirName, fileName ))
          } else {
            None
          }
		}
	}
}
