/*
 *  TimelineAxis.scala
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

import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter
import session.Timeline
import math._
import util.Model
import de.sciss.span.Span
import legacy.ComponentHost
import de.sciss.audiowidgets.j.Axis
import desktop.impl.DynamicComponentImpl

class TimelineAxis( view: TimelineView, host: Option[ ComponentHost ])
extends Axis() with DynamicComponentImpl
/* with DynamicListening */ {

  protected def dynamicComponent = this

  format = Axis.Format.Time()
//  componentHost = host  // XXX TODO

//	private var isListening		= false
//	private var editorVar: Option[ TimelineView#Editor ] = None
    private var timelineVis = view.span

		// --- Listener ---
    private val mil = new MouseInputAdapter() {
        	// when the user begins a selection by shift+clicking, the
        	// initially fixed selection bound is saved to selectionStart.
        	private var selectionStart = -1L
        	private var shiftDrag       = false
            private var altDrag         = false

        	override def mousePressed( e: MouseEvent ) {
        		shiftDrag		= e.isShiftDown
        		altDrag			= e.isAltDown
        		selectionStart  = -1L
        		dragTimelinePosition( e )
        	}

        	override def mouseDragged( e: MouseEvent ) {
        		dragTimelinePosition( e )
        	}

      private def dragTimelinePosition(e: MouseEvent) {
        view.editor.foreach(ed => {
          // translate into a valid time offset
          val position = view.timeline.span.clip(
            timelineVis.start + ((e.getX.toDouble / getWidth) * timelineVis.length).toLong)

          val ce = ed.editBegin(getResourceString("editTimelineView"))

          //println( "HIER : " + position + "; view.span " + view.span + "; timeline.span " + view.timeline.span )

          if (shiftDrag) {
            view.selection.span match {
              case _ if altDrag =>
                selectionStart = view.cursor.position
                altDrag = false
              case Span.Void =>
                selectionStart = view.cursor.position
              case Span(selStart, selStop) if selectionStart == -1 =>
                selectionStart =
                  if (abs(selStart - position) > abs(selStop - position))
                    selStart
                  else selStop
              case _ =>
            }
            val newSelSpan = Span(min(position, selectionStart),
              max(position, selectionStart))
            ed.editSelect(ce, newSelSpan)
          } else {
            if (altDrag) {
              ed.editSelect(ce, Span.Void)
              ed.editPosition(ce, position)
              altDrag = false
            } else {
              ed.editPosition(ce, position)
            }
          }
          ed.editEnd(ce)
        })
      }
    }

     private val timelineListener: Model.Listener = {
        case Timeline.RateChanged( _, _ ) => recalcSpace( trigger = true )
        // note: viewport does not necessarily repaint when
        // view sizes changes, for whatever reason. so we
        // need to repaint in any case here
        case TimelineView.SpanChanged( _, newSpan ) /* if( viewPort.isEmpty )*/ =>
          if( newSpan != timelineVis ) {
            timelineVis = newSpan
            recalcSpace( trigger = true )
         }
     }

// XXX TODO
//  // note that the view rect change might be _before_
//  // the delivery of TimelineView.SpanChanged, therefore
//  // we need to synthesize the span from the view rect!
//  override protected def viewRectChanged(r: Rectangle) {
//    val tlSpan  = view.timeline.span
//    val w       = getWidth
//    val scale   = tlSpan.length.toDouble / w
//    val start   = (r.x * scale + 0.5).toLong + tlSpan.start
//    val stop    = (r.width * scale + 0.5).toLong + start
//    timelineVis = Span(start, stop)
//    recalcSpace(trigger = false)
//  }

  // ---- constructor ----
//  setFont(application.getGraphicsHandler.getFont(GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_MINI))

  if (view.editor.isDefined) {
    //println( "EDITOR" )
    addMouseListener(mil)
    addMouseMotionListener(mil)
  }

  // XXX TODO
//  new DynamicAncestorAdapter( this ).addTo( this )

/*
    def editor: Option[ TimelineView#Editor ] = editorVar
	def editor_=( newEditor: Option[ TimelineView#Editor ]) {
		if( editorVar != newEditor ) {
			if( (editorVar.isEmpty ) && newEditor.isDefined ) {
				addMouseListener( mil )
				addMouseMotionListener( mil )
			} else if( editorVar.isDefined && newEditor.isEmpty ) {
				removeMouseListener( mil )
				removeMouseMotionListener( mil )
			}
			editorVar = newEditor
		}
	}
*/

  private def recalcSpace(trigger: Boolean) {
//    val spc = if ((format & Axis.Format.) == 0) {
//      VectorSpace.createLinSpace(timelineVis.start,
//        timelineVis.stop,
//        0.0, 1.0, null, null, null, null)
//    } else {
      val d1 = 1.0 / view.timeline.rate
    val start = timelineVis.start * d1
    val stop  = timelineVis.stop * d1
//      VectorSpace.createLinSpace(start, stop, 0.0, 1.0, null, null, null, null)
    minimum = start
    maximum = stop
//    }
//    if (trigger) {
//      space = spc
//    } else {
//      setSpaceNoRepaint(spc)
//    }
  }

  // ---------------- DynamicListening interface ----------------

    protected def componentShown() {
      view.addListener( timelineListener )
      timelineVis = view.span
      recalcSpace( trigger = true )
    }

    protected def componentHidden() {
      view.removeListener( timelineListener )
    }

	protected def getResourceString( key: String ) =
		key // XXX TODO AbstractApplication.getApplication.getResourceString( key )

	// -------------- Disposable interface --------------

//	override def dispose() {
//		stopListening()
////		editor = null
//		super.dispose()
//	}
}