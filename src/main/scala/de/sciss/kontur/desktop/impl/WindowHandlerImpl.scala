package de.sciss.kontur
package desktop
package impl

import swing.{Action, Component, Dialog}
import javax.swing.{JOptionPane, RootPaneContainer, SwingUtilities}
import java.util.StringTokenizer

object WindowHandlerImpl {
  def findWindow(component: Component): Option[Window] = {
    val rp = SwingUtilities.getAncestorOfClass(classOf[RootPaneContainer], component.peer)
    if (rp == null) return None
    val w = rp.asInstanceOf[RootPaneContainer].getRootPane.getClientProperty("de.sciss.desktop.Window")
    if (w == null) return None
    Some(w.asInstanceOf[Window])
  }

  def showDialog(parent: Component, dialog: Dialog) {
    findWindow(parent) match {
      case Some(w)  => w.handler.showDialog(w, dialog)
      case _        => showDialog(dialog)
    }
  }

  def showDialog(dialog: Dialog) {
    dialog.open()
  }

  def showDialog(parent: Component, pane: JOptionPane, title: String): Any = {
    findWindow(parent) match {
      case Some(w)  => w.handler.showDialog(w, pane, title)
      case _        => showDialog(pane, title)
    }
  }

  def showDialog(pane: JOptionPane, title: String): Any = {
    val jdlg  = pane.createDialog(title)
    val owner = new Window { val peer = jdlg }
    val dlg   = new Dialog(owner)
    showDialog(dlg)
    pane.getValue
  }

  def showErrorDialog(exception: Exception, title: String) {
    val strBuf = new StringBuffer("Exception: ")
    val message = if (exception == null) "null" else (exception.getClass.getName + " - " + exception.getLocalizedMessage)
    var lineLen = 0
    val options = Array[AnyRef]("Ok", "Show Stack Trace")
    val tok = new StringTokenizer(message)
    strBuf.append(":\n")
    while (tok.hasMoreTokens) {
      val word = tok.nextToken()
      if (lineLen > 0 && lineLen + word.length() > 40) {
        strBuf.append("\n")
        lineLen = 0
      }
      strBuf.append(word)
      strBuf.append(' ')
      lineLen += word.length() + 1
    }
    val op = new JOptionPane(strBuf.toString, JOptionPane.ERROR_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options(0))
    if (showDialog(op, title) == 1) {
      exception.printStackTrace()
    }
  }

  def showAction(window: Window): Action = new ShowAction(window)

  private final class ShowAction(window: Window) extends Action(window.title) {
    window.reactions += {
      case Window.Activated(_) =>
//				if( !disposed ) {
          // ((BasicApplication) AbstractApplication.getApplication()).getMenuFactory().setSelectedWindow( ShowWindowAction.this );
          ???
//			  }
    }

	  def apply() {
      window.visible = true
      window.front()
    }

//    def dispose() {
//      w.reactions -= ...
//    }
	}

  private final class DialogWindow(dialog: Dialog) extends WindowImpl {
// 			if( modal ) fph.addModalDialog(); // this shit is necessary because java.awt.FileDialog doesn't fire windowActivated ...
    visible = true
// 			if( modal ) fph.removeModalDialog();
    dispose()
  }
}
final class WindowHandlerImpl(val application: SwingApplication) extends WindowHandler {
  import WindowHandlerImpl._

  def showDialog(window: Window, dialog: Dialog) {
 		// temporarily disable alwaysOnTop
 		val wasOnTop = if (!usesInternalFrames && usesFloatingPalettes) windows.filter { w =>
       val res = window.alwaysOnTop
       if (res) window.alwaysOnTop = false
       res
    } .toList else Nil

 		try {
 			new DialogWindow(dialog)
 		} finally { // make sure to restore original state
       wasOnTop.foreach(_.alwaysOnTop = true)
 		}
 	}
}