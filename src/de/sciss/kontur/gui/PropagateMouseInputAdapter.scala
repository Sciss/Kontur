/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Component, Container }
import java.awt.event.{ MouseEvent, MouseListener, MouseMotionListener }
import javax.swing.{ SwingUtilities }
import javax.swing.event.{ MouseInputAdapter }

class PropagateMouseInputAdapter private( parent: Option[ Container ])
extends MouseInputAdapter {
    def this( parent: Container ) = this( Some( parent ))
    def this() = this( None )

    override def mousePressed(  e: MouseEvent ) = propagate( e )
    override def mouseReleased( e: MouseEvent ) = propagate( e )
    override def mouseMoved(    e: MouseEvent ) = propagate( e )
    override def mouseDragged(  e: MouseEvent ) = propagate( e )

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