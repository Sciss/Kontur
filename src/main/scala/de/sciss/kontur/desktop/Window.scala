package de.sciss.kontur.desktop

import swing.{MenuBar, Component}
import javax.swing.WindowConstants
import java.awt.{Insets, Point, Rectangle, Dimension}
import java.awt.event.WindowEvent
import javax.swing.event.InternalFrameEvent
import java.io.File

object Window {
  sealed trait Style
  /** Regular full-fledged window. */
  case object Regular   extends Style
  /** Supplementary window which for example might not need menu bar. */
  case object Auxiliary extends Style
  /** Supplementary window which is a (possibly floating) palette. */
  case object Palette   extends Style

  sealed trait CloseOperation { def id: Int }
  case object CloseIgnore  extends CloseOperation { val id = WindowConstants.DO_NOTHING_ON_CLOSE  }
  case object CloseExit    extends CloseOperation { val id = WindowConstants.EXIT_ON_CLOSE        }
  case object CloseHide    extends CloseOperation { val id = WindowConstants.HIDE_ON_CLOSE        }
  case object CloseDispose extends CloseOperation { val id = WindowConstants.DISPOSE_ON_CLOSE     }

//  trait Listener {
// 		def windowOpened(e: Event): Unit
//    def windowClosing(e: Event): Unit
//    def windowClosed(e: Event): Unit
//    def windowIconified(e: Event): Unit
//    def windowDeiconified(e: Event): Unit
//    def windowActivated(e: Event): Unit
//    def windowDeactivated(e: Event): Unit
//    // def windowGainedFocus(e: Event): Unit
//    // def windowLostFocus(e: Event): Unit
//  }

// 	trait Adapter extends Listener {
//    def windowOpened(e: Event) {}
//    def windowClosing(e: Event) {}
//    def windowClosed(e: Event) {}
//    def windowIconified(e: Event) {}
//    def windowDeiconified(e: Event) {}
//    def windowActivated(e: Event) {}
//    def windowDeactivated(e: Event) {}
//      // def windowGainedFocus(e: Event) {}
//      // def windowLostFocus(e: Event) {}
//  }

  object Event {
    final val WINDOW_FIRST        = WindowEvent.WINDOW_FIRST
    final val WINDOW_OPENED       = WindowEvent.WINDOW_OPENED
    final val WINDOW_CLOSING      = WindowEvent.WINDOW_CLOSING
    final val WINDOW_CLOSED       = WindowEvent.WINDOW_CLOSED
    final val WINDOW_ICONIFIED    = WindowEvent.WINDOW_ICONIFIED
    final val WINDOW_DEICONIFIED  = WindowEvent.WINDOW_DEICONIFIED
    final val WINDOW_ACTIVATED    = WindowEvent.WINDOW_ACTIVATED
    final val WINDOW_DEACTIVATED  = WindowEvent.WINDOW_DEACTIVATED
    // final val WINDOW_GAINED_FOCUS  = WindowEvent.WINDOW_GAINED_FOCUS
    // final val WINDOW_LOST_FOCUS    = WindowEvent.WINDOW_LOST_FOCUS

    def apply(w: Window, e: WindowEvent): Event = new Event(w, e.getID)
    def apply(w: Window, e: InternalFrameEvent) = new Event(w, e.getID - InternalFrameEvent.INTERNAL_FRAME_FIRST + WINDOW_FIRST)
  }
  final class Event(val window: Window, id: Int) extends java.awt.AWTEvent(window, id)
}
/** Interface that unites functionality
  *	from inhomogeneous classes such as JFrame, JDialog, JInternalFrame
  */
trait Window {
  var content: Component
  var title: String
  var closeOperation: Window.CloseOperation
  var visible: Boolean
  var undecorated: Boolean

  def component: Component

  def pack(): Unit
  def dispose(): Unit
  def front(): Unit
//  def revalidate(): Unit

  def isFloating: Boolean
  def isActive: Boolean
  var resizable: Boolean
  var dirty: Boolean
  var file: Option[File]
  var alpha: Float
//  var focusTraversalKeysEnabled: Boolean

  var size: Dimension
  var bounds: Rectangle
  var location: Point
  var menubar: MenuBar
  def insets: Insets

//	def init(): Unit

//	def addListener(l: Listener): Listener
//	def removeListener(l: Listener): Listener

//	def inputMap(condition: Int): InputMap
//	def actionMap: ActionMap

//  def ownedWindows: Iterator[java.awt.Window]

//	def setLocationRelativeTo(c: Component)
}