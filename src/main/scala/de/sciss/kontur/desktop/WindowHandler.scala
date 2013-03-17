package de.sciss.kontur.desktop

import swing.{Component, Dialog}
import impl.{WindowHandlerImpl => Impl}
import javax.swing.{JOptionPane, JFrame, RootPaneContainer, JInternalFrame, SwingUtilities}
import java.awt.{Rectangle, GraphicsEnvironment, Toolkit}

object WindowHandler {
//  final val OPTION_EXCLUDE_FONT: AnyRef = "excludefont"
//  final val OPTION_GLOBAL_MENUBAR: AnyRef = "globalmenu"

  def findWindow(component: Component): Option[Window] = Impl.findWindow(component)

  def showDialog(dialog: Dialog)                                           { Impl.showDialog(dialog) }
  def showDialog(parent: Component, dialog: Dialog)                        { Impl.showDialog(parent, dialog) }
  def showDialog(parent: Component, pane: JOptionPane, title: String): Any = Impl.showDialog(parent, pane, title)
  def showDialog(pane: JOptionPane, title: String): Any =                    Impl.showDialog(pane, title)
  def showErrorDialog(exception: Exception, title: String)                 { Impl.showErrorDialog(exception, title) }

  def menuShortcut: Int = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask

  def availableSpace: Rectangle = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
}
trait WindowHandler {
  def application: SwingApplication

  def addWindow   (w: Window): Unit
  def removeWindow(w: Window): Unit

  def windows: Iterator[Window]

//  def createWindow(flags: Int): Window

  def usesInternalFrames: Boolean
  def usesScreenMenuBar: Boolean
  def usesFloatingPalettes: Boolean

  def setDefaultBorrower(w: Window): Unit

  def showDialog(window: Window, dialog: Dialog): Unit
  def showDialog(window: Window, pane: JOptionPane, title: String): Unit
}