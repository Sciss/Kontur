/*
 *  SessionTreeFrame.scala
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

import session.Session
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.{DropMode, JScrollPane, JTree, ScrollPaneConstants}
import swing.{Frame, Component}
import de.sciss.desktop.impl.WindowImpl
import de.sciss.desktop.Window

class SessionTreeFrame(val document: Session) extends WindowImpl with SessionFrame {
  frame =>

  def handler = Kontur.windowHandler

  private val sessionTreeModel = new SessionTreeModel(document)

  // ---- constructor ----
  {
    // ---- menus and actions ----
    // val mr = app.getMenuBarRoot

    val ggTree = new JTree(sessionTreeModel)
    val wrapTree = Component.wrap(ggTree)
    ggTree.setDropMode(DropMode.ON_OR_INSERT)
    ggTree.setRootVisible(false)
    // ggTree.setShowsRootHandles( true )
    val ggScroll = new JScrollPane(ggTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

    ggTree.addMouseListener(new MouseAdapter() {
      override def mousePressed(e: MouseEvent): Unit = {
        val selRow = ggTree.getRowForLocation(e.getX, e.getY)
        if (selRow == -1) return
        val selPath = ggTree.getPathForLocation(e.getX, e.getY)
        val node = selPath.getLastPathComponent

        if (e.isPopupTrigger) popup(node, e)
        else if (e.getClickCount == 2) doubleClick(node, e)
      }

      private def popup(node: AnyRef, e: MouseEvent): Unit =
        node match {
          case hcm: HasContextMenu =>
            hcm.createContextMenu().foreach { root =>
              // XXX TODO - internal frame
              frame.component match {
                case f: Frame =>
                  val pop = root.create(frame)
                  pop.show(wrapTree, e.getX, e.getY)
              }
            }
          case _ =>
        }

      private def doubleClick(node: AnyRef, e: MouseEvent): Unit =
        node match {
          case hdca: HasDoubleClickAction => hdca.doubleClickAction()
          case _ =>
        }
    })
    new TreeDragSource(ggTree)
    new TreeDropTarget(ggTree)

    contents = Component.wrap(ggScroll)
    //      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!

    // XXX TODO
//      addDynamicListening( sessionTreeModel )
    sessionTreeModel.startListening()

//      init()

//      initBounds	// be sure this is after documentUpdate!

    visible = true
  }

  protected def windowClosing(): Unit = {
    sessionTreeModel.stopListening()
    close()
  }

//  override protected def autoUpdatePrefs  = true
//  override protected def alwaysPackSize   = false

  protected def elementName = Some("Tree")
}
