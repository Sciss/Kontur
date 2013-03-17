package de.sciss.kontur
package desktop
package impl

import swing.{Component, Dialog}
import javax.swing.{JOptionPane, RootPaneContainer, SwingUtilities}

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
      case _        =>
        dialog.open()
    }
  }

  def showDialog(parent: Component, pane: JOptionPane, title: String) {
    findWindow(parent) match {
      case Some(w)  => w.handler.showDialog(w, pane, title)
      case _        => ???
    }
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