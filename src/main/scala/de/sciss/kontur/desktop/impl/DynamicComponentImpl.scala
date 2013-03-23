package de.sciss.kontur.desktop
package impl

import java.awt.event.{ComponentEvent, ComponentListener, WindowListener, WindowEvent}
import javax.swing.event.{AncestorEvent, AncestorListener}
import java.awt
import javax.swing.JComponent

trait DynamicComponentImpl {
  private var listening   = false
  private var win         = Option.empty[awt.Window]

  protected def dynamicComponent: JComponent
  protected def componentShown (): Unit
  protected def componentHidden(): Unit

  final protected def isListening = listening

  // ---- constructor ----
  dynamicComponent.addAncestorListener(listener)
  learnWindow(Option(dynamicComponent.getTopLevelAncestor))

  private def startListening() {
    if (!listening) {
      listening = true
      componentShown()
    }
 	}

  private def stopListening() {
    if (listening) {
      listening = false
      componentHidden()
    }
  }

  private def forgetWindow() {
    win.foreach { w =>
      w.removeWindowListener(listener)
      w.removeComponentListener(listener)
      win = None
      stopListening()
    }
  }

  private def learnWindow(c: Option[awt.Container]) {
    c match {
      case Some(w: awt.Window) =>
        win = Some(w)
        w.addWindowListener(listener)
        w.addComponentListener(listener)
        if (w.isShowing) startListening()

      case _ =>
    }
 	}

  private object listener extends WindowListener with ComponentListener with AncestorListener {
    def windowOpened     (e: WindowEvent) { startListening() }
 		def windowClosed     (e: WindowEvent) { stopListening () }

    def windowClosing    (e: WindowEvent) {}
    def windowIconified  (e: WindowEvent) {}
    def windowDeiconified(e: WindowEvent) {}
    def windowActivated  (e: WindowEvent) {}
    def windowDeactivated(e: WindowEvent) {}

    def componentShown  (e: ComponentEvent) { startListening() }
    def componentHidden (e: ComponentEvent) { stopListening () }

    def componentResized(e: ComponentEvent) {}
    def componentMoved  (e: ComponentEvent) {}

    def ancestorAdded(e: AncestorEvent) {
      val c = Option(e.getComponent.getTopLevelAncestor)
      if (c != win) {
        forgetWindow()
        learnWindow(c)
      }
    }

    def ancestorRemoved(e: AncestorEvent) { forgetWindow() }

    def ancestorMoved(event: AncestorEvent) {}
  }

//	def remove() {
//		removeAncestorListener(listener)
//		forgetWindow()
//	}
}