/*
 *  SessionTreeFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

  protected def style = Window.Regular

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
      override def mousePressed(e: MouseEvent) {
        val selRow = ggTree.getRowForLocation(e.getX, e.getY)
        if (selRow == -1) return
        val selPath = ggTree.getPathForLocation(e.getX, e.getY)
        val node = selPath.getLastPathComponent

        if (e.isPopupTrigger) popup(node, e)
        else if (e.getClickCount == 2) doubleClick(node, e)
      }

      private def popup(node: AnyRef, e: MouseEvent) {
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
      }

      private def doubleClick(node: AnyRef, e: MouseEvent) {
        node match {
          case hdca: HasDoubleClickAction => hdca.doubleClickAction()
          case _ =>
        }
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

  protected def windowClosing() {
    sessionTreeModel.stopListening()
    close()
  }

//  override protected def autoUpdatePrefs  = true
//  override protected def alwaysPackSize   = false

  protected def elementName = Some("Tree")
}
