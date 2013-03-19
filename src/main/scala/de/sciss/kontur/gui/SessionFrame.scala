/*
 *  SessionFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.kontur
package gui

import java.awt.{FileDialog, Frame}
import java.awt.event.{ActionEvent, KeyEvent}
import java.io.{File, IOException}
import javax.swing.{SwingUtilities, AbstractAction, JComponent, JOptionPane, KeyStroke}
import session.Session
import util.{Flag, Model}
import swing.Action
import de.sciss.desktop.Window
import de.sciss.desktop.impl.WindowImpl

trait SessionFrame {
   frame: WindowImpl =>

  protected def application: de.sciss.desktop.Application { type Document = Session }

   private var writeProtected	= false
   private var wpHaveWarned	= false

   private val actionShowWindow= Window.showAction(this)
   protected val actionClose  = new ActionClose()
   private val actionSave     = new ActionSave()
   private val actionSaveAs	= new ActionSaveAs( false )

// XXX TODO
//   private val winListener = new AbstractWindow.Adapter() {
//        override def windowClosing( e: AbstractWindow.Event ) {
//            frame.windowClosing()
//        }
//
//        override def windowActivated( e: AbstractWindow.Event ) {
//            // need to check 'disposed' to avoid runtime exception in doc handler if document was just closed
//            if( !disposed ) {
//                application.documentHandler.activeDocument = Some(document)
//                handler.setMenuBarBorrower( frame )
//            }
//        }
//    }

  private val docListener: Model.Listener = {
    case Session.DirtyChanged(_)    => updateTitle()
    case Session.PathChanged(_, _)  => updateTitle()
  }

  private var disposed = false; // compiler problem, we cannot name it disposed

  // ---- constructor ----
  {
    // ---- menus and actions ----
    val mr = application.getMenuBarRoot
    mr.putMimic("file.close",  this, actionClose)
    mr.putMimic("file.save",   this, actionSave)
    mr.putMimic("file.saveAs", this, actionSaveAs)

    mr.putMimic("edit.undo", this, document.undoManager.undoAction)
    mr.putMimic("edit.redo", this, document.undoManager.redoAction)

    updateTitle()
    document.addListener(docListener)

    application.getMenuFactory.addToWindowMenu(actionShowWindow) // MUST BE BEFORE INIT()!!
    closeOperation = Window.CloseIgnore
    // XXX TODO
    //      addListener( winListener )
  }

  def document: Session

   protected def elementName: Option[ String ]

   protected def windowClosing() : Unit

   protected def invokeDispose() {
      disposed = true	// important to avoid "too late window messages" to be processed; fucking swing doesn't kill them despite listener being removed
     // XXX TODO
//      removeListener( winListener )
     document.removeListener( docListener )
      application.getMenuFactory.removeFromWindowMenu( actionShowWindow )
// XXX TODO
//      actionShowWindow.dispose()
      dispose()
   }

  /**
   * Recreates the main frame's title bar
   * after a sessions name changed (clear/load/save as session)
   */
  protected def updateTitle() {
    writeProtected = document.path.map(!_.canWrite) getOrElse false

    val name = document.displayName
    title = (if (!handler.usesInternalFrames) handler.application.name else "") +
      (if (document.dirty) " - \u2022" else " - ") + name + (elementName.map(e => " - " + e) getOrElse "")

    actionShowWindow.title = name
    actionSave.enabled = !writeProtected && document.dirty
    dirty = document.dirty
    file  = document.path

    //		final AudioFileInfoPalette infoBox = (AudioFileInfoPalette) app.getComponent( Main.COMP_AUDIOINFO )
    //		if( infoBox != null ) infoBox.updateDocumentName( doc )

    if (writeProtected && !wpHaveWarned && document.dirty) {
      val op = new JOptionPane(getResourceString("warnWriteProtected"), JOptionPane.WARNING_MESSAGE)
      showDialog(op -> "Warning") // getResourceString("msgDlgWarn"))
      wpHaveWarned = true
    }
  }

  private def getResourceString(key: String): String = key  // XXX TODO

  //   override def dispose() {
//      frame.dispose()
//   }

   protected def documentClosed() {
     // XXX TODO
//      application.documentHandler.removeDocument(doc)	// XXX
      invokeDispose() // dispose()
   }

   def closeDocument( force: Boolean, wasClosed: Flag ) {
//      doc.getTransport().stop();
      val okToClose = force || {
         val name = "Close" // getResourceString( "menuClose" )
//         if( !confirmCancel( name )) {
//            wasClosed.set( false );
//            return null;
//         }
         val saved = confirmUnsaved( name, wasClosed )
//         if( pt != null ) {
//            pt.addListener( new ProcessingThread.Listener() {
//               public void processStarted( ProcessingThread.Event e ) { /* ignored */ }
//               public void processStopped( ProcessingThread.Event e )
//               {
//                  if( e.isDone() ) {
//                     documentClosed();
//                  }
//               }
//            });
//            return pt;
//         }
         saved || wasClosed() // aka confirmed.isSet
      }
//println( "wasClosed : " + wasClosed.isSet )
      if( okToClose ) {
         documentClosed()
      }
//      return null;
   }


   /*
    *  Checks if there are unsaved changes to
    *  the session. If so, displays a confirmation
    *  dialog. Invokes Save/Save As depending
    *  on user selection. IF the doc was not dirty,
    *	or if &quot;Cancel&quot; or
    *	&quot;Don't save&quot; was chosen, the
    *	method returns <code>null</code> and the
    *	<code>confirmed</code> flag reflects whether
    *	the document should be closed. If a saving
    *	process should be started, that process is
    *	returned. Note that the <code>ProcessingThread</code>
    *	in this case has not yet been started, as to
    *	allow interested objects to install a listener
    *	first. So it's their job to call the <code>start</code>
    *	method!
    *
    *  @param  actionName		name of the action that
    *							threatens the session
    *	@param	confirmed		a flag that will be set to <code>true</code> if
    *							the doc is allowed to be closed
    *							(doc was not dirty or user chose &quot;Don't save&quot;),
    *							otherwise <code>false</code> (save process
    *							initiated or user chose &quot;Cancel&quot;).
    *  @return				true is file was saved
    *
    *	@see	de.sciss.eisenkraut.util.ProcessingThread#start
    */
   def confirmUnsaved(actionName: String, confirmed: Flag): Boolean = {
     if (!document.dirty) {
       confirmed.value = true
       return false
     }

     val options = Array[AnyRef]("Save...", "Cancel", "Don't Save"
       // getResourceString( "buttonSave" ),
       // getResourceString( "buttonCancel" ),
       // getResourceString( "buttonDontSave" )
     )
     val dont = Flag.False()
     val name = document.displayName

     val op = new JOptionPane(s"<html><body><p><b>Do you want to save the changes you made in\nthe document $name?</b></p>" +
       "<p>Your changes will be lost if you don't save them.</p></body></html>",
       JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null,
       options, options(1))
//     val d = op.createDialog(component.peer, actionName)
//     val rp = d.getRootPane
//     if (rp != null) {
//       rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
//         KeyStroke.getKeyStroke(KeyEvent.VK_D, Window.menuShortcut), "dont")
//       rp.getActionMap.put("dont", new AbstractAction {
//         def actionPerformed(e: ActionEvent) {
//           dont() = true
//           d.dispose()
//         }
//       })
//     }
     op.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
       KeyStroke.getKeyStroke(KeyEvent.VK_D, Window.menuShortcut), "dont")
     op.getActionMap.put("dont", new AbstractAction {
       def actionPerformed(e: ActionEvent) {
         val w = SwingUtilities.getWindowAncestor(op)
         if (w != null) {
           dont() = true
           w.dispose()
         }
       }
     })
     showDialog(op -> actionName)
     val choice = if (dont()) {
       2
     } else {
       val value = op.getValue
       if ((value == null) || (value == options(1))) {
         1
       } else if (value == options(0)) {
         0
       } else if (value == options(2)) {
         2
       } else {
         -1 // throws assertion error in switch block
       }
     }

     choice match {
       case JOptionPane.CLOSED_OPTION | 1 =>
         confirmed() = false
         false

       case 2 =>
         // don't save
         confirmed() = true
         false

       case 0 =>
         confirmed() = false
         val path = if (document.path.isEmpty || writeProtected) {
           actionSaveAs.query(actionSave.title)
         } else {
           document.path
         }
         path.map(p => {
           actionSave.perform(actionSave.title, p, asCopy = false, openAfterSave = false)
         }) getOrElse false
     }
   }

   protected class ActionClose extends Action("Close") {
     def apply() {
       closeDocument(force = false, Flag.False())
     }
   }

   // action for the Save-Session menu item
   private class ActionSave
    extends Action("Save") {
      /**
       *  Saves a Session. If the file
       *  wasn't saved before, a file chooser
       *  is shown before.
       */
      def actionPerformed( e: ActionEvent ) {
         val name = title
         (document.path orElse actionSaveAs.query( name )).foreach( f =>
            perform( name, f, asCopy = false, openAfterSave = false ))
      }

      protected[gui] def perform( name: String, file: File, asCopy: Boolean, openAfterSave: Boolean ) : Boolean = {
         try {
           document.save( file )
            wpHaveWarned = false

            if( !asCopy ) {
               application.getMenuFactory.addRecent( file )
              document.path = Some( file )
              document.undoManager.clear()
            }
            if( openAfterSave ) {
               application.getMenuFactory.openDocument( file )
            }
            true
         }
         catch { case e1: IOException =>
            showDialog( e1 -> name )
            false
         }
      }
   }

  // action for the Save-Session-As menu item
  private class ActionSaveAs(asCopy: Boolean)
    extends Action("Save As") {

    private val openAfterSave = Flag.False()

    /*
     *  Query a file name from the user and save the Session
     */
    def apply() {
      val name = title
      query(name).foreach { f =>
        actionSave.perform(name, f, asCopy = asCopy, openAfterSave = openAfterSave())
      }
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
     protected[gui] def query(name: String): Option[File] = {
       val dlg = new FileDialog(null.asInstanceOf[Frame], name, FileDialog.SAVE)
       Window.showDialog(dlg)
     }
  }
}