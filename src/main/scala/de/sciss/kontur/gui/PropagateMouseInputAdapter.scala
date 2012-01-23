/*
 *  PropagateMouseInputAdapter.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2012 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.kontur.gui

import java.awt.{ Component, Container }
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.event.MouseInputAdapter

class PropagateMouseInputAdapter private( parent: Option[ Container ])
extends MouseInputAdapter {
    def this( parent: Container ) = this( Some( parent ))
    def this() = this( None )

    override def mousePressed(  e: MouseEvent ) { propagate( e )}
    override def mouseReleased( e: MouseEvent ) { propagate( e )}
    override def mouseMoved(    e: MouseEvent ) { propagate( e )}
    override def mouseDragged(  e: MouseEvent ) { propagate( e )}

    def propagate( e: MouseEvent ) {
        val p = parent getOrElse e.getComponent.getParent
        val ec = SwingUtilities.convertMouseEvent( e.getComponent, e, p )
        p.dispatchEvent( ec )
    }

    def removeFrom( c: Component ) {
        c.addMouseListener( this )
        c.addMouseMotionListener( this )
    }

    def addTo( c: Component ) {
        c.removeMouseListener( this )
        c.removeMouseMotionListener( this )
    }
}