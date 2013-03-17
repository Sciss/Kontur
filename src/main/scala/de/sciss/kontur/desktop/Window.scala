package de.sciss.kontur.desktop

import swing.{Reactions, MenuBar, Component}
import javax.swing.WindowConstants
import java.awt.{Insets, Point, Rectangle, Dimension}
import java.awt.event.WindowEvent
import javax.swing.event.InternalFrameEvent
import java.io.File
import annotation.switch

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
    def apply(window: Window, peer: WindowEvent): Event = {
      import WindowEvent._
      (peer.getID: @switch) match {
        case WINDOW_ACTIVATED   => WindowActivated  (window)
        case WINDOW_CLOSED      => WindowClosed     (window)
        case WINDOW_CLOSING     => WindowClosing    (window)
        case WINDOW_DEACTIVATED => WindowDeactivated(window)
        case WINDOW_DEICONIFIED => WindowDeiconified(window)
        case WINDOW_ICONIFIED   => WindowIconified  (window)
        case WINDOW_OPENED      => WindowOpened     (window)
      }
    }
    def apply(window: Window, peer: InternalFrameEvent): Event = {
      import InternalFrameEvent._
      (peer.getID: @switch) match {
        case INTERNAL_FRAME_ACTIVATED   => WindowActivated  (window)
        case INTERNAL_FRAME_CLOSED      => WindowClosed     (window)
        case INTERNAL_FRAME_CLOSING     => WindowClosing    (window)
        case INTERNAL_FRAME_DEACTIVATED => WindowDeactivated(window)
        case INTERNAL_FRAME_DEICONIFIED => WindowDeiconified(window)
        case INTERNAL_FRAME_ICONIFIED   => WindowIconified  (window)
        case INTERNAL_FRAME_OPENED      => WindowOpened     (window)
      }
    }
  }
  sealed trait Event extends swing.event.Event {
    def source: Window
  }

  final case class WindowActivated  (source: Window) extends Event
  final case class WindowClosed     (source: Window) extends Event
  final case class WindowClosing    (source: Window) extends Event
  final case class WindowDeactivated(source: Window) extends Event
  final case class WindowDeiconified(source: Window) extends Event
  final case class WindowIconified  (source: Window) extends Event
  final case class WindowOpened     (source: Window) extends Event
}
/** Interface that unites functionality
  *	from inhomogeneous classes such as JFrame, JDialog, JInternalFrame
  */
trait Window {
  def handler: WindowHandler

  var contents: Component
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
  var alwaysOnTop: Boolean
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

  def reactions: Reactions
}