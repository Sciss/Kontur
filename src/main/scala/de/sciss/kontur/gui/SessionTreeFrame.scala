/*
 *  SessionTreeFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import de.sciss.app.AbstractWindow
import de.sciss.kontur.session.Session
import java.awt.BorderLayout
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.{ DropMode, JScrollPane, JTree, ScrollPaneConstants }

class SessionTreeFrame( protected val doc: Session )
extends AppWindow( AbstractWindow.REGULAR ) with SessionFrame {
   frame =>

   // ---- constructor ----
   {
      // ---- menus and actions ----
//		val mr = app.getMenuBarRoot

      val cp = getContentPane
      val sessionTreeModel = new SessionTreeModel( doc )
      val ggTree = new JTree( sessionTreeModel )
      ggTree.setDropMode( DropMode.ON_OR_INSERT )
      ggTree.setRootVisible( false )
//    ggTree.setShowsRootHandles( true )
      val ggScroll = new JScrollPane( ggTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		                         	   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )

      ggTree.addMouseListener( new MouseAdapter() {
       override def mousePressed( e: MouseEvent ) {
          val selRow  = ggTree.getRowForLocation( e.getX, e.getY )
          if( selRow == -1 ) return
          val selPath = ggTree.getPathForLocation( e.getX, e.getY )
          val node = selPath.getLastPathComponent

          if( e.isPopupTrigger ) popup( node, e )
          else if( e.getClickCount == 2 ) doubleClick( node, e )
       }
       
        private def popup( node: AnyRef, e: MouseEvent ) {
           node match {
              case hcm: HasContextMenu => {
                  hcm.createContextMenu().foreach( root => {
                      val pop = root.createPopup( frame )
                      pop.show( e.getComponent, e.getX, e.getY )
                  })
              }
                case _ =>
            }
        }

        private def doubleClick( node: AnyRef, e: MouseEvent ) {
           node match {
              case hdca: HasDoubleClickAction => hdca.doubleClickAction()
              case _ =>
           }
        }
      })
      new TreeDragSource( ggTree )
      new TreeDropTarget( ggTree )

      cp.add( ggScroll, BorderLayout.CENTER )
//      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!

      addDynamicListening( sessionTreeModel )

      init()

//      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
   }

   protected def windowClosing() { actionClose.perform() }

   override protected def autoUpdatePrefs = true
   override protected def alwaysPackSize = false

   protected def elementName = Some( "Tree" )
}
