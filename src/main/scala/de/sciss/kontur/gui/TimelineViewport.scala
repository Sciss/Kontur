/*
 *  TimelineViewport.scala
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

import java.awt.{Dimension, Point, Rectangle}
import javax.swing.{JComponent, JViewport}
import util.Model
import de.sciss.span.Span
import desktop.impl.DynamicComponentImpl

final class TimelineViewport(timelineView: TimelineView)
  extends JViewport with DynamicComponentImpl {

  var verbose = false

  protected def dynamicComponent = this

  // ---- constructor ----
//  {
//     new DynamicAncestorAdapter( this ).addTo( this )
//  }

  protected def componentShown() {
     timelineView.addListener( timelineViewListener )
  }

  protected def componentHidden() {
     timelineView.removeListener( timelineViewListener )
  }

  private val timelineViewListener: Model.Listener = {
      case TimelineView.SpanChanged( oldSpan, newSpan ) => {
//         val dim = getPreferredSize()
         val tlSpan = timelineView.timeline.span
         if( !tlSpan.isEmpty && !newSpan.isEmpty ) {
//             val scale = dim.getWidth.toDouble / tlSpan.getLength
             val e = getExtentSize
if( verbose ) println( "e.w " + e.width + "; tl.len " + tlSpan.length + "; visi.len " + newSpan.length )
             val w = (tlSpan.length.toDouble / newSpan.length * e.width + 0.5).toInt
             val x = ((newSpan.start - tlSpan.start).toDouble / tlSpan.length * w + 0.5).toInt
             val d = getViewSize
             val p = getViewPosition
if( verbose ) println( "old.x" + p.x + "; old.w " + d.width + "; new.x " + x + "; new.w " + w )
             if( w != d.width ) {
                  val v = getView
                  val pref = v.getPreferredSize
                  pref.width = w
                  v.setPreferredSize( pref )
                  v.asInstanceOf[ JComponent ].revalidate() // bad bad
                  d.width = w
                  super.setViewSize( d )
             }
             if( x != p.x ) {
                super.setViewPosition( p )
             }
         }
      }
  }

//  override protected def fireStateChanged() {
//    println( "---VP : fireStateChanged()" )
//    super.fireStateChanged()
//  }

  override def scrollRectToVisible( contentRect: Rectangle ) {
    if( verbose ) {
      println( "---VP : scrollRectToVisible( new Rectangle( " +
        contentRect.x + ", " + contentRect.y + ", " +
        contentRect.width + ", " + contentRect.height + " ))" )
    }
    super.scrollRectToVisible( contentRect )
  }

  override def setExtentSize( newExtent: Dimension ) {
    if( verbose ) {
      println( "---VP: setExtentSize( new Dimension( " +
        newExtent.width + ", " + newExtent.height + " ))" )
    }
    super.setExtentSize( newExtent )
  }

// OOO
  override def setViewPosition( p: Point ) {
//     println( "---VP: setViewPosition( new Point( " + p.x + ", " + p.y + " ))" )
      val tlSpan  = timelineView.timeline.span
      val w       = getViewSize.width
      val scale   = tlSpan.length.toDouble / w
      val r       = getViewRect
      val start   = (r.x * scale + 0.5).toLong + tlSpan.start
      val stop    = (r.width * scale + 0.5).toLong + start
      val newSpan = Span( start, stop )
      if( newSpan != timelineView.span ) {
         timelineView.editor.foreach( ed => {
             val ce = ed.editBegin( "scroll" )
             ed.editScroll( ce, Span( start, stop ))
             ed.editEnd( ce )
        })
      } else {  // vertical scrolling
         super.setViewPosition( p )
      }
  }

  override def setViewSize( newSize: Dimension ) {
     if( verbose ) {
       println( "---VP: setViewSize( new Dimension( " +
         newSize.width + ", " + newSize.height + " ))" )
     }
     super.setViewSize( newSize )
  }
}
