/*
 *  SessionFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package gui

import java.awt.event.{ActionEvent, KeyEvent}
import java.io.{File, IOException}
import javax.swing.{SwingUtilities, AbstractAction, JComponent, JOptionPane, KeyStroke}
import session.Session
import util.{Flag, Model}
import swing.Action
import de.sciss.desktop.{FileDialog, OptionPane, Window}
import de.sciss.desktop.impl.WindowImpl

trait SessionFrame {
  frame: WindowImpl =>

  // protected def application: SwingApplication {type Document = Session}

  private   var writeProtected    = false
  private   var wpHaveWarned      = false

  private   val actionShowWindow  = Window.Actions.show(this)
  private   val actionSaveAs      = new ActionSaveAs(false)

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

  private var disposed = false

  bindMenus(
    "file.close"  -> ActionClose,
    "file.save"   -> ActionSave,
    "file.saveAs" -> actionSaveAs,

    "edit.undo"   -> document.undoManager.undoAction,
    "edit.redo"   -> document.undoManager.redoAction
  )

  updateTitle()
  document.addListener(docListener)

  // XXX TODO
  //    application.getMenuFactory.addToWindowMenu(actionShowWindow) // MUST BE BEFORE INIT()!!
  closeOperation = Window.CloseIgnore
  reactions += {
    case Window.Closing(_) => frame.windowClosing()
    case Window.Activated(_) => Kontur.documentHandler.activeDocument = Some(document)
  }

  def document: Session

  protected def elementName: Option[String]

  protected def windowClosing(): Unit

  protected def invokeDispose(): Unit = {
    disposed = true // important to avoid "too late window messages" to be processed; fucking swing doesn't kill them despite listener being removed
    // XXX TODO
    //      removeListener( winListener )
    document.removeListener(docListener)
    // XXX TODO
    //      application.getMenuFactory.removeFromWindowMenu( actionShowWindow )
    // XXX TODO
//    actionShowWindow.dispose()
    dispose()
  }

  final def close(): Unit = ActionClose()

  /** Recreates the main frame's title bar
    * after a sessions name changed (clear/load/save as session)
    */
  protected def updateTitle(): Unit = {
    writeProtected = document.path.map(!_.canWrite) getOrElse false

    val name = document.displayName
    title = (if (!handler.usesInternalFrames) handler.application.name else "") +
      (if (document.dirty) " - \u2022" else " - ") + name + (elementName.map(e => " - " + e) getOrElse "")

    actionShowWindow.title = name
    ActionSave.enabled = !writeProtected && document.dirty
    dirty = document.dirty
    file  = document.path

    //		final AudioFileInfoPalette infoBox = (AudioFileInfoPalette) app.getComponent( Main.COMP_AUDIOINFO )
    //		if( infoBox != null ) infoBox.updateDocumentName( doc )

    if (writeProtected && !wpHaveWarned && document.dirty) {
      val op  = OptionPane.message(message = getResourceString("warnWriteProtected"),
        messageType = OptionPane.Message.Warning)
      op.title = "Warning"
      showDialog(op) // getResourceString("msgDlgWarn"))
      wpHaveWarned = true
    }
  }

  private def getResourceString(key: String): String = key  // XXX TODO

  //   override def dispose() {
//      frame.dispose()
//   }

   protected def documentClosed(): Unit = {
     // XXX TODO
//      application.documentHandler.removeDocument(doc)	// XXX
      invokeDispose() // dispose()
   }

  def closeDocument(force: Boolean, wasClosed: Flag): Unit = {
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

     val op = OptionPane(message = s"<html><body><p><b>Do you want to save the changes you made in<br>the document &ldquo;$name&rdquo;?</b>" +
            "<p><p><small>Your changes will be lost if you don't save them.</small></body></html>",
       messageType = OptionPane.Message.Warning, optionType = OptionPane.Options.YesNoCancel, entries = options, initial = Some(options(1)))

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
     op.peer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
       KeyStroke.getKeyStroke(KeyEvent.VK_D, Window.menuShortcut), "dont")
     op.peer.getActionMap.put("dont", new AbstractAction {
       def actionPerformed(e: ActionEvent): Unit = {
         val w = SwingUtilities.getWindowAncestor(op.peer)
         if (w != null) {
           dont() = true
           w.dispose()
         }
       }
     })
     op.title = actionName
     showDialog(op)
     val choice = if (dont()) {
       2
     } else {
       val value = op.peer.getValue
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
           actionSaveAs.query(ActionSave.title)
         } else {
           document.path
         }
         path.map(p => {
           ActionSave.perform(ActionSave.title, p, asCopy = false, openAfterSave = false)
         }) getOrElse false
     }
   }

   private object ActionClose extends Action("Close") {
     def apply(): Unit = closeDocument(force = false, Flag.False())
   }

   // action for the Save-Session menu item
   private object ActionSave
    extends Action("Save") {
      /** Saves a Session. If the file
        * wasn't saved before, a file chooser
        * is shown before.
        */
      def apply(): Unit = {
         val name = title
         (document.path orElse actionSaveAs.query( name )).foreach( f =>
            perform( name, f, asCopy = false, openAfterSave = false ))
      }

      protected[gui] def perform( name: String, file: File, asCopy: Boolean, openAfterSave: Boolean ) : Boolean = {
         try {
           document.save( file )
            wpHaveWarned = false

            if( !asCopy ) {
// XXX TODO
//               application.getMenuFactory.addRecent( file )
              document.path = Some( file )
              document.undoManager.clear()
            }
            if( openAfterSave ) {
               GlobalActions.openDocument( file )
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
    def apply(): Unit = {
      val name = title
      query(name).foreach { f =>
        ActionSave.perform(name, f, asCopy = asCopy, openAfterSave = openAfterSave())
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
      // val dlg = new FileDialog(null.asInstanceOf[Frame], name, FileDialog.SAVE)
      val dlg = FileDialog.save(title = name)
      Window.showDialog(dlg)
    }
  }
}