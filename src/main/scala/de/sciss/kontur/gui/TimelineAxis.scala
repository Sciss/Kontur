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
import de.sciss.app.{ AbstractApplication, DynamicAncestorAdapter, DynamicListening, GraphicsHandler }
import de.sciss.gui.{ /* Axis,*/ ComponentHost, VectorSpace }
import de.sciss.io.Span
import session.Timeline
import math._
import util.Model

class TimelineAxis( view: TimelineView, host: Option[ ComponentHost ])
extends Axis( Axis.HORIZONTAL, Axis.TIMEFORMAT, host )
with DynamicListening {

	private var isListening		= false
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

        	private def dragTimelinePosition( e: MouseEvent ) {
                view.editor.foreach( ed => {
                  // translate into a valid time offset
                  val position  = view.timeline.span.clip(
                    timelineVis.start + ((e.getX.toDouble / getWidth) * timelineVis.getLength).toLong )

                  val ce = ed.editBegin( getResourceString( "editTimelineView" ))

//println( "HIER : " + position + "; view.span " + view.span + "; timeline.span " + view.timeline.span )

                  if( shiftDrag ) {
                    val selSpan = view.selection.span
        			if( altDrag || selSpan.isEmpty ) {
        				selectionStart = view.cursor.position
        				altDrag = false
        			} else if( selectionStart == -1 ) {
        				selectionStart =
                          if( abs( selSpan.start - position ) > abs( selSpan.stop - position ))
        					selSpan.start else selSpan.stop
        			}
        			val newSelSpan = new Span( min( position, selectionStart ),
        								max( position, selectionStart ))
        			ed.editSelect( ce, newSelSpan )
                } else {
        			if( altDrag ) {
        				ed.editSelect( ce, new Span() )
        				ed.editPosition( ce, position )
        				altDrag = false
        			} else {
        				ed.editPosition( ce, position )
        			}
                }
                ed.editEnd( ce )
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

    // note that the view rect change might be _before_
    // the delivery of TimelineView.SpanChanged, therefore
    // we need to synthesize the span from the view rect!
    override protected def viewRectChanged( r: Rectangle ) {
        val tlSpan      = view.timeline.span
        val w           = getWidth
        val scale       = tlSpan.getLength.toDouble / w
        val start       = (r.x * scale + 0.5).toLong + tlSpan.start
        val stop        = (r.width * scale + 0.5).toLong + start
        timelineVis = new Span( start, stop )
        recalcSpace( trigger = false )
    }

    // ---- constructor ----
    {
 		val app = AbstractApplication.getApplication
 		setFont( app.getGraphicsHandler.getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_MINI ))

        if( view.editor.isDefined ) {
//println( "EDITOR" )
            addMouseListener( mil )
        	addMouseMotionListener( mil )
        }

        new DynamicAncestorAdapter( this ).addTo( this )
    }
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
	private def recalcSpace( trigger: Boolean ) {
// println( "TimelineAxis : recalcSpace. visi = " + visibleSpan )
        val spc = if( (flags & Axis.TIMEFORMAT) == 0 ) {
			VectorSpace.createLinSpace( timelineVis.start,
										timelineVis.stop,
									  	0.0, 1.0, null, null, null, null )
		} else {
			val d1 = 1.0 / view.timeline.rate
			VectorSpace.createLinSpace( timelineVis.start * d1,
										timelineVis.stop * d1,
									    0.0, 1.0, null, null, null, null )
		}
        if( trigger ) {
          space = spc
        } else {
          setSpaceNoRepaint( spc )
        }
	}

// ---------------- DynamicListening interface ----------------

    def startListening() {
    	if( !isListening ) {
//println( "TIMELINE AXIS START")
    		isListening = true
    		view.addListener( timelineListener )
            timelineVis = view.span
    		recalcSpace( trigger = true )
    	}
    }

    def stopListening() {
    	if( isListening ) {
    		isListening = false
    		view.removeListener( timelineListener )
    	}
    }

	protected def getResourceString( key: String ) =
		AbstractApplication.getApplication.getResourceString( key )

	// -------------- Disposable interface --------------

	override def dispose() {
		stopListening()
//		editor = null
		super.dispose()
	}
}