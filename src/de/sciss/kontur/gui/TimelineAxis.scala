/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.event.{ MouseEvent }
import javax.swing.event.{ MouseInputAdapter, MouseInputListener }
import de.sciss.app.{ AbstractApplication, Application, DynamicAncestorAdapter,
                        DynamicListening, GraphicsHandler }
import de.sciss.gui.{ Axis, ComponentHost, VectorSpace }
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ TimelineElement }
import scala.math._

class TimelineAxis( view: TimelineView, host: ComponentHost )
extends Axis( Axis.HORIZONTAL, Axis.TIMEFORMAT, host )
with DynamicListening {

	private var isListening		= false
	private var editorVar: Option[ TimelineView#Editor ] = None

		// --- Listener ---
    private val mil = new MouseInputAdapter() {
        	// when the user begins a selection by shift+clicking, the
        	// initially fixed selection bound is saved to selectionStart.
        	private var selectionStart = -1L
        	private var shiftDrag       = false
            private var altDrag         = false

        	override def mousePressed( e: MouseEvent ) {
        		shiftDrag		= e.isShiftDown()
        		altDrag			= e.isAltDown()
        		selectionStart  = -1L
        		dragTimelinePosition( e )
        	}

        	override def mouseDragged( e: MouseEvent ) {
        		dragTimelinePosition( e )
        	}

        	private def dragTimelinePosition( e: MouseEvent ) {
                editorVar.foreach( ed => {
                  // translate into a valid time offset
                  val position  = view.timeline.span.clip(
                    view.span.start + (e.getX().toDouble / (getWidth * view.span.getLength)).toLong )

                  val id = ed.editBegin( TimelineAxis.this, getResourceString( "editTimelineView" ))

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
        			ed.editSelect( id, newSelSpan )
                } else {
        			if( altDrag ) {
        				ed.editSelect( id, new Span() )
        				ed.editPosition( id, position )
        				altDrag = false
        			} else {
        				ed.editPosition( id, position )
        			}
                }
                ed.editEnd( id )
        	})
            }
        }

     private val timelineListener = (x: AnyRef) => x match {
//        case TimelineElement.SpanChanged => recalcSpace
        case TimelineElement.RateChanged => recalcSpace
        case TimelineView.SpanChanged => recalcSpace
        case _ =>
     }

    // ---- constructor ----
    {
 		val app = AbstractApplication.getApplication()
 		setFont( app.getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_MINI ))

        new DynamicAncestorAdapter( this ).addTo( this )
    }

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

	private def recalcSpace {
    	val visibleSpan = view.span
        val space = if( (getFlags() & Axis.TIMEFORMAT) == 0 ) {
			VectorSpace.createLinSpace( visibleSpan.start,
										    visibleSpan.stop,
									    	0.0, 1.0, null, null, null, null )
		} else {
			val d1 = 1.0 / view.timeline.rate
			VectorSpace.createLinSpace( visibleSpan.start * d1,
											visibleSpan.stop * d1,
										    0.0, 1.0, null, null, null, null )
		}
		setSpace( space )
	}

// ---------------- DynamicListening interface ----------------

    def startListening() {
    	if( !isListening ) {
    		isListening = true
    		view.addListener( timelineListener )
    		recalcSpace
    	}
    }

    def stopListening() {
    	if( isListening ) {
    		isListening = false
    		view.removeListener( timelineListener )
    	}
    }

	protected def getResourceString( key: String ) =
		AbstractApplication.getApplication().getResourceString( key )

	// -------------- Disposable interface --------------

	override def dispose() {
		stopListening
		editor = null
		super.dispose()
	}
}