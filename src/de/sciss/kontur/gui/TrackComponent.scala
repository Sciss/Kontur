/*
 *  TrackComponent.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
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

import java.awt.{ Color, Dimension, Graphics, Graphics2D }
import javax.swing.{ JComponent, Spring, SpringLayout }
import de.sciss.kontur.session.{ Region, Track }
import de.sciss.app.{ AbstractApplication, GraphicsHandler }

class DefaultTrackComponent( t: Track, tracksView: TracksView, timelineView: TimelineView )
extends JComponent {

    {
//        val rnd = new java.util.Random()
//        setBackground( Color.getHSBColor( rnd.nextFloat(), 0.5f, 1f ))
/*        val dim = getPreferredSize()
        dim.height = 64 // XXX
        setPreferredSize( dim )
        val dim2 = getPreferredSize()
        dim2.height = 64 // XXX
        setMinimumSize( dim2 )
        val dim3 = getPreferredSize()
        dim3.height = 64 // XXX
        setMaximumSize( dim3 ) */

//        val lay = new SpringLayout()
//        val cons = lay.getConstraints( this )
//        cons.setWidth( Spring.constant( 64 ))
//        cons.setHeight( Spring.constant( 64 ))  // XXX
      setFont( AbstractApplication.getApplication().getGraphicsHandler()
        .getFont( GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI ))
    }

    override def getPreferredSize() : Dimension = {
       val dim = super.getPreferredSize()
       dim.height = 64
       dim
    }

    override def getMinimumSize() : Dimension = {
       val dim = super.getMinimumSize()
       dim.height = 64
       dim
    }

    override def getMaximumSize() : Dimension = {
       val dim = super.getMaximumSize()
       dim.height = 64
       dim
    }

    override def paintComponent( g: Graphics ) {
//       g.setColor( getBackground )
//       g.fillRect( 0, 0, getWidth, getHeight )

       val r = getBounds()
       val off = -timelineView.timeline.span.start
       val scale = getWidth.toDouble / timelineView.timeline.span.getLength

       t.trail.visitRange( timelineView.span )( stake => {
//           println( "found " + stake )
            r.x     = ((stake.span.start + off) * scale + 0.5).toInt
            r.width = ((stake.span.stop + off) * scale + 0.5).toInt - r.x
            g.setColor( Color.black )
            g.fillRect( r.x, r.y, r.width, r.height )
            val clip = g.getClip
            g.clipRect( r.x, r.y, r.width, r.height )
            g.setColor( Color.white )
            stake match {
              case reg: Region => g.drawString( reg.name, r.x + 4, r.y + 12 )
              case _ =>
            }
       })
    }
}
