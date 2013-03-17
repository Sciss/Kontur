package de.sciss.kontur.desktop
package impl

import java.awt.{Insets, Point, Rectangle, Dimension}
import java.io.File
import javax.swing.RootPaneContainer
import swing.{Dialog, Component}

object WindowImpl {
  object Delegate {
    def apply(): Delegate = ???
  }
  sealed trait Delegate {
    var contents: Component
    def component: Component
    var closeOperation: Window.CloseOperation
    var title: String
    var resizable: Boolean
    def putRootPaneProperty(name: String, value: Any): Unit
    def pack(): Unit
  }
}
trait WindowImpl extends Window {
  import WindowImpl._

  protected def application: SwingApplication = handler.application

  final def size = component.size
  final protected def size_=(value: Dimension) { component.peer.setSize(value) }
  final def bounds = component.bounds
  final protected def bounds_=(value: Rectangle) { component.peer.setBounds(value) }
  final def location = component.location
  final protected def location_=(value: Point) { component.peer.setLocation(value) }
  final def title = delegate.title
  final protected def title_=(value: String) { delegate.title = value }
  final def resizable = delegate.resizable
  final protected def resizable_=(value: Boolean) { delegate.resizable = value }
  final protected def closeOperation = delegate.closeOperation
  final protected def closeOperation_=(value: Window.CloseOperation) { delegate.closeOperation = value }

  final protected def pack() { delegate.pack() }
  final protected def contents = delegate.contents
  final protected def contents_=(value: Component) { delegate.contents = value }

  private final val delegate: Delegate = {
    ???
  }

  private var _file = Option.empty[File]
  final def file = _file
  final def file_=(value: Option[File]) {
    _file = value
    delegate.putRootPaneProperty("Window.documentFile", value.orNull)
  }

  private var _alpha = 1f
  final def alpha = _alpha
  final def alpha_=(value: Float) {
    _alpha = value
    delegate.putRootPaneProperty("Window.alpha", value)
    delegate.putRootPaneProperty("apple.awt.draggableWindowBackground", false)
  }

  final protected def makeUnifiedLook() {
    delegate.putRootPaneProperty("apple.awt.brushMetalLook", true)
  }

  final def component: Component = delegate.component

//  def insets: Insets = {
//    if (w != null) {
//      w.getInsets()
//    } else if (jif != null) {
//      jif.getInsets()
//    } else {
//      throw new IllegalStateException()
//    }
//  }

  protected def showDialog(dialog: Dialog) {
 		handler.showDialog(this, dialog)
 	}
}

trait DefaultWindowImpl extends WindowImpl {
  final protected def style = Window.Regular

  handler.setDefaultBorrower(this)
}