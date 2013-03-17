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
        case WINDOW_ACTIVATED   => Activated  (window)
        case WINDOW_CLOSED      => Closed     (window)
        case WINDOW_CLOSING     => Closing    (window)
        case WINDOW_DEACTIVATED => Deactivated(window)
        case WINDOW_DEICONIFIED => Deiconified(window)
        case WINDOW_ICONIFIED   => Iconified  (window)
        case WINDOW_OPENED      => Opened     (window)
      }
    }
    def apply(window: Window, peer: InternalFrameEvent): Event = {
      import InternalFrameEvent._
      (peer.getID: @switch) match {
        case INTERNAL_FRAME_ACTIVATED   => Activated  (window)
        case INTERNAL_FRAME_CLOSED      => Closed     (window)
        case INTERNAL_FRAME_CLOSING     => Closing    (window)
        case INTERNAL_FRAME_DEACTIVATED => Deactivated(window)
        case INTERNAL_FRAME_DEICONIFIED => Deiconified(window)
        case INTERNAL_FRAME_ICONIFIED   => Iconified  (window)
        case INTERNAL_FRAME_OPENED      => Opened     (window)
      }
    }
  }
  sealed trait Event extends swing.event.Event {
    def source: Window
  }

  final case class Activated  (source: Window) extends Event
  final case class Closed     (source: Window) extends Event
  final case class Closing    (source: Window) extends Event
  final case class Deactivated(source: Window) extends Event
  final case class Deiconified(source: Window) extends Event
  final case class Iconified  (source: Window) extends Event
  final case class Opened     (source: Window) extends Event
}
/** Interface that unites functionality
  *	from inhomogeneous classes such as JFrame, JDialog, JInternalFrame
  */
trait Window {
  def handler: WindowHandler

//  protected var contents: Component
  def title: String
//  protected def title_=(value: String): Unit

//  protected var closeOperation: Window.CloseOperation
  var visible: Boolean
//  protected def setUndecorated(value: Boolean): Unit

  def component: Component

//  protected def pack(): Unit
  def dispose(): Unit
  def front(): Unit
//  def revalidate(): Unit

  def floating: Boolean
  def active: Boolean

  def resizable: Boolean
//  protected def resizable_=(value: Boolean): Unit

//  protected var dirty: Boolean
//  protected var file: Option[File]
//  protected var alpha: Float

  var alwaysOnTop: Boolean
//  var focusTraversalKeysEnabled: Boolean

  def size: Dimension
//  protected def size_=(value: Dimension): Unit
  def bounds: Rectangle
//  protected def bounds_=(value: Rectangle): Unit

  var location: Point
//  var menubar: MenuBar
//  def insets: Insets

//	def init(): Unit

//	def addListener(l: Listener): Listener
//	def removeListener(l: Listener): Listener

//	def inputMap(condition: Int): InputMap
//	def actionMap: ActionMap

//  def ownedWindows: Iterator[java.awt.Window]

//	def setLocationRelativeTo(c: Component)

  def reactions: Reactions
}