/*
 *  TimelinePanel.scala
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

import java.awt.{ Color, Graphics, Graphics2D, Rectangle }
import java.awt.event.{ ActionEvent, ActionListener }
import javax.swing.{ BoxLayout, JComponent, JViewport, Timer }
import javax.swing.event.{ ChangeEvent, ChangeListener }
import scala.math._

import de.sciss.gui.{ ComponentHost, TopPainter }
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ Marker, SessionElementSeq, Stake, Timeline,
                                Track, Trail }

//import Track.Tr

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.14, 09-Jan-10
 */
//object TimelinePanel {
//}

class TimelinePanel( /* val tracksView: TracksView,*/ val timelineView: TimelineView )
extends JComponent // ComponentHost
with TopPaintable {
	private val colrSelection			= new Color( 0x00, 0x00, 0xFF, 0x4F ); // GraphicsUtil.colrSelection;
	private val colrPosition			= new Color( 0xFF, 0x00, 0x00, 0x7F );
	private val colrSelection2			= new Color( 0x00, 0x00, 0x00, 0x40 );  // selected timeline span over unselected trns
	private var vpRecentRect			= new Rectangle()
	private var vpPosition				= -1
	private val vpPositionRect          = new Rectangle()
	private var vpSelections: List[ ViewportSelection ] = Nil
	private val vpSelectionRect			= new Rectangle()
	private var vpUpdateRect			= new Rectangle()
	private var vpScale                  = 1f

	private var timelineVis             = timelineView.span
	private var timelineSel             = timelineView.selection.span
	protected var timelinePos           = timelineView.cursor.position
	protected var timelineRate          = timelineView.timeline.rate
//    private val tracks = tracksView.tracks

//	private final Timer							playTimer;

	// !!! for some crazy reason, these need to be volatile because otherwise
	// the playTimer's actionPerformed body might use a cached value !!!
	// how can this happen when javax.swing.Timer is playing on the event thread?!
//	protected double							playRate				= 1.0;
//	protected long								playStartPos			= 0;
//	protected long								playStartTime;
//	protected boolean							isPlaying				= false;

//	val timelineAxis = new TimelineAxis( timelineView, Some( this ))
//	val markerAxis = new MarkerAxis( timelineView, this )
	private var markerTrail: Option[ Trail[ Marker ]] = None

	protected val markVisible = true

	private var trackListVar: TrackList = new DummyTrackList
    private var viewPortVar: Option[ JViewport ] = None

    private val timelineListener = (msg: AnyRef) => msg match {
      case TimelineCursor.PositionChanged( _, newPos ) => {
//println( "POSITION CHANGED " + oldPos + " -> " + newPos )
		timelinePos = newPos
		updatePositionAndRepaint
      }
      case TimelineView.SpanChanged( _, newSpan ) => {
        if( timelineVis != newSpan ) {
/*
          if( viewPortVar.isDefined ) {
             if( timelineVis.getLength != newSpan.getLength ) {
                 calcPreferredSize
             }
             calcViewPortRect
          }
          timelineVis	= newSpan
*/
          updateTransformsAndRepaint( false )
        }
      }
      case TimelineSelection.SpanChanged( oldSpan, newSpan ) => {
   		timelineSel	= newSpan
		updateSelectionAndRepaint
		if( oldSpan.isEmpty != newSpan.isEmpty ) {
//			updateEditEnabled( !newSpan.isEmpty )
		}
      }
//      case Timeline.SpanChanged( _, newSpan ) if( viewPortVar.isDefined ) => calcPreferredSize
      case Timeline.RateChanged( _, newRate ) => {
		timelineRate = newRate
//		playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
      }
    }

    private val trackListListener = (msg: AnyRef) => msg match {
      case TrackList.ElementAdded( idx, elem ) => updateSelectionAndRepaint
      case TrackList.ElementRemoved( idx, elem ) => updateSelectionAndRepaint
      case TrackList.SelectionChanged( _ @ _* ) => {
//println( "....gugu" )
          updateSelectionAndRepaint
      }
    }

//    private val markerListener = (msg: AnyRef) => msg match {
//      case Trail.Changed( span ) => repaintMarkers( span )
//    }

   // ---- constructor ----
	{
//		addTopPainter( this )
		setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ))

//		add( timelineAxis )
//		add( markerAxis )
/*
		playTimer = new Timer( 33, new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				// the swing timer doesn't have a cancel method,
				// hence events already scheduled will be delivered
				// even if stop is called between firing and delivery(?)
				if( !isPlaying ) return;

				timelinePos = (long) ((System.currentTimeMillis() - playStartTime) * timelineRate * playRate / 1000 + playStartPos);
				updatePositionAndRepaint();
			}
		});
*/
		// ---------- Listeners ----------

		timelineView.addListener( timelineListener )
//        tracksView.addListener( tracksViewListener )
/*
		markAxis.addListener( new MarkerAxis.Listener() {
			public void markerDragStarted( MarkerAxis.Event e )
			{
				addCatchBypass();
			}

			public void markerDragStopped( MarkerAxis.Event e )
			{
				removeCatchBypass();
			}

			public void markerDragAdjusted( MarkerAxis.Event e )
			{
				repaintMarkers( e.getModificatioSpan() );
			}
		});
      */
	}

    def viewPort = viewPortVar
    def viewPort_=( newPort: Option[ JViewport ]) {
//        viewPortVar.foreach( _.removeChangeListener( viewPortListener ))
        viewPortVar = newPort
//        viewPortVar.foreach( _.addChangeListener( viewPortListener ))
    }

/*
    def tracksViewListener( tracks: SessionElementSeq[ Track ])( msg: AnyRef ) : Unit = msg match {
      case tracks.ElementAdded( idx, elem ) => updateSelectionAndRepaint
      case tracks.ElementRemoved( idx, elem ) => updateSelectionAndRepaint
      case TracksView.SelectionChanged( _ ) => updateSelectionAndRepaint
    }
*/

    def trackList = trackListVar
    def trackList_=( newTL: TrackList ) {
      trackListVar.removeListener( trackListListener )
      trackListVar = newTL
      trackListVar.addListener( trackListListener )
      updateSelectionAndRepaint
    }

	protected def repaintMarkers( affectedSpan: Span ) {
		if( !markVisible || !affectedSpan.touches( timelineVis )) return

		val span = affectedSpan.shift( -timelineVis.start )
		val updateRect = new Rectangle(
			(span.start * vpScale).toInt, 0,
			(span.getLength() * vpScale).toInt + 2, getHeight() ).
				intersection( new Rectangle( 0, 0, getWidth(), getHeight() ))
		if( !updateRect.isEmpty() ) {
			// update markAxis in any case, even if it's invisible
			// coz otherwise the flag stakes are not updated!
//			update( markAxis );
			repaint( updateRect )
		}
	}

    private val normalRect = new Rectangle
    private def normalBounds: Rectangle = {
        normalRect.x      = 0
        normalRect.y      = 0
        normalRect.width  = getWidth
        normalRect.height = getHeight
        normalRect
    }

    private def portBounds: Rectangle = {
// println( "vp rect = " + viewPort.get.getViewRect )
      val r = viewPort.get.getViewRect
      if( r != normalRect ) {
          normalRect.setBounds( r )
          viewRectChanged( r )
      }
      normalRect
    }

    // subclasses might want to use this
    protected def viewRectChanged( r: Rectangle ) {}

//	override def paintComponent( g: Graphics ) {}
	override def paintChildren( g: Graphics ) {
//        super.paintComponent( g )
        super.paintChildren( g )
        
        val g2 = g.asInstanceOf[ Graphics2D ]
//        val r = if( viewPort.isEmpty ) normalBounds else portBounds
        val r = normalBounds

        if( vpRecentRect != r ) {
            if( vpRecentRect.height != r.height ) updateSelection
			recalcTransforms( r )
		}

        vpSelections.foreach( vps => {
			g2.setColor( vps.color )
			g2.fillRect( vpSelectionRect.x, vps.bounds.y - vpRecentRect.y, vpSelectionRect.width, vps.bounds.height )
		})

//		if( markVisible ) {
//			markerAxis.paintFlagSticks( g2, vpRecentRect )
//		}

		g2.setColor( colrPosition )
		g2.drawLine( vpPosition, 0, vpPosition, vpRecentRect.height )

        paintOnTop( g2 )
	}

/*
	public void play( long startPos, double rate )
	{
		playStartPos	= startPos; // timelinePos;
		//System.out.println( "play : " + playStartPos );
		playRate		= rate;
		playStartTime	= System.currentTimeMillis();
		playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
		isPlaying		= true;
		playTimer.restart();
	}

	public void setPlayRate( long startPos, double rate )
	{
		if( !isPlaying ) return;

		playStartPos	= startPos; // timelinePos;
		playRate		= rate;
		playStartTime	= System.currentTimeMillis();
		playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
	}

	public void stop()
	{
		isPlaying = false;
		playTimer.stop();
	}
*/
	/* override */ def dispose {
		timelineView.removeListener( timelineListener )
//        tracksView.removeListener( tracksViewListener )
        trackList = new DummyTrackList
//		markerTrack = None
//		this.stop

//		super.dispose
	}

	private def recalcTransforms( newRect: Rectangle ) {
		vpRecentRect = newRect // getViewRect();

        val span = timelineView.timeline.span // whole line, _not_ view

		if( !span.isEmpty ) {
			vpScale = (vpRecentRect.width.toDouble / max( 1, span.getLength )).toFloat
//			playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
			vpPosition		= ((timelinePos - span.start) * vpScale + 0.5f).toInt
			vpPositionRect.setBounds( vpPosition, 0, 1, vpRecentRect.height )
			if( !timelineSel.isEmpty ) {
				val x			= ((timelineSel.start - span.start) * vpScale + 0.5f).toInt + vpRecentRect.x
				val w			= max( 1, ((timelineSel.stop - span.start) * vpScale + 0.5f).toInt - x )
				vpSelectionRect.setBounds( x, 0, w, vpRecentRect.height )
			} else {
				vpSelectionRect.setBounds( 0, 0, 0, 0 )
			}
		} else {
			vpScale			= 0.0f
			vpPosition		= -1
			vpPositionRect.setBounds( 0, 0, 0, 0 )
			vpSelectionRect.setBounds( 0, 0, 0, 0 )
		}
	}

	protected def updatePositionAndRepaint {
		val pEmpty = (vpPositionRect.x + vpPositionRect.width < 0) || (vpPositionRect.x > vpRecentRect.width)
		if( !pEmpty ) vpUpdateRect.setBounds( vpPositionRect )

        val span = timelineView.timeline.span // whole line, _not_ view

		if( vpScale > 0f ) {
			vpPosition	= ((timelinePos - span.start) * vpScale + 0.5f).toInt
			// choose update rect such that even a paint manager delay of 200 milliseconds
			// will still catch the (then advanced) position so we don't see flickering!
			// XXX this should take playback rate into account, though
			vpPositionRect.setBounds( vpPosition, 0, Math.max( 1, (vpScale * timelineRate * 0.2f).toInt ), vpRecentRect.height )
		} else {
			vpPosition	= -1
			vpPositionRect.setBounds( 0, 0, 0, 0 )
		}

		val cEmpty = (vpPositionRect.x + vpPositionRect.width <= 0) || (vpPositionRect.x > vpRecentRect.width)
		if( pEmpty ) {
			if( cEmpty ) return
			val x   = max( 0, vpPositionRect.x )
			val x2  = min( vpRecentRect.width, vpPositionRect.x + vpPositionRect.width )
			vpUpdateRect.setBounds( x, vpPositionRect.y, x2 - x, vpPositionRect.height )
		} else {
			if( cEmpty ) {
				val x   = Math.max( 0, vpUpdateRect.x )
				val x2  = Math.min( vpRecentRect.width, vpUpdateRect.x + vpUpdateRect.width )
				vpUpdateRect.setBounds( x, vpUpdateRect.y, x2 - x, vpUpdateRect.height )
			} else {
				val x   = Math.max( 0, Math.min( vpUpdateRect.x, vpPositionRect.x ));
				val x2  = Math.min( vpRecentRect.width, Math.max( vpUpdateRect.x + vpUpdateRect.width,
															vpPositionRect.x + vpPositionRect.width ))
				vpUpdateRect.setBounds( x, vpUpdateRect.y, x2 - x, vpUpdateRect.height )
			}
		}
		if( !vpUpdateRect.isEmpty ) {
			repaint( vpUpdateRect )
		}
	}

	/**
	 *  Only call in the Swing thread!
	 */
	protected def updateSelectionAndRepaint {
		val r = new Rectangle( 0, 0, getWidth, getHeight )

		vpUpdateRect.setBounds( vpSelectionRect )
		recalcTransforms( r )
		updateSelection
		if( vpUpdateRect.isEmpty() ) {
			vpUpdateRect.setBounds( vpSelectionRect )
		} else if( !vpSelectionRect.isEmpty ) {
			vpUpdateRect = vpUpdateRect.union( vpSelectionRect )
		}
		vpUpdateRect = vpUpdateRect.intersection( new Rectangle( 0, 0, getWidth, getHeight ))
		if( !vpUpdateRect.isEmpty ) {
			repaint( vpUpdateRect )
		}
	}

	private def updateTransformsAndRepaint( verticalSelection: Boolean ) {
		val r = new Rectangle( 0, 0, getWidth, getHeight )

		vpUpdateRect = vpSelectionRect.union( vpPositionRect )
		recalcTransforms( r )
		if( verticalSelection ) updateSelection
		vpUpdateRect = vpUpdateRect.union( vpPositionRect ).union( vpSelectionRect ).intersection( r )
		if( !vpUpdateRect.isEmpty ) {
			repaint( vpUpdateRect )	// XXX ??
		}
	}

	// sync: caller must sync on timeline + grp + tc
	private def updateSelection {
		vpSelections = Nil

    	if( timelineSel.isEmpty ) return

//        vpSelections ::= ViewportSelection( timelineAxis.getBounds, colrSelection )
 
        trackList.foreach( elem => {
           val r = trackList.getBounds( elem )
           vpSelections ::= ViewportSelection( r,
               if( elem.selected ) colrSelection else colrSelection2 )
        })
    }

/*
    private def viewPortListener = new ChangeListener {
      def stateChanged( e: ChangeEvent ) {
        viewPortVar.foreach( vp => {
//          println( "CHANGE : " + e.getSource )
          val tlSpan  = timelineView.timeline.span
          val w       = getWidth
          val scale   = tlSpan.getLength.toDouble / w
          val r       = vp.getViewRect
          val start   = (r.x * scale + 0.5).toLong + tlSpan.start
          val stop    = (r.width * scale + 0.5).toLong + start
          val newSpan = new Span( start, stop )
          if( newSpan != timelineVis ) {
            timelineView.editor.foreach( ed => {
                val ce = ed.editBegin( "scroll" )
                ed.editScroll( ce, new Span( start, stop ))
                ed.editEnd( ce )
            })
          }
        })
      }
    }
*/

/*
  private def calcPreferredSize {
     val vp = viewPortVar.get
     val dim = getPreferredSize()
     val vw = vp.getExtentSize.width
     val tlLen = timelineView.timeline.span.getLength
     val vLen  = timelineView.span.getLength
     if( tlLen == 0 || vLen == 0 ) return
     val scale = tlLen.toDouble / vLen
     dim.width = (vw * scale + 0.5).toInt
     
//     dim.height = 0
//     var i = 0; while( i < getComponentCount() ) {
//       val c = getComponent( i )
//       dim.height += c.getPreferredSize().height
//       i += 1
//     }
       setPreferredSize( dim )
//println( "PREFERRED 1 : " + dim )
     revalidate()
     val columnHeader = trackList.columnHeaderView
     val dim2 = columnHeader.getPreferredSize
     dim2.width = dim.width
     columnHeader.setPreferredSize( dim2 )
     columnHeader.revalidate()
  }
*/

/*
  private def calcViewPortRect {
     val vp = viewPortVar.get
     val dim = getPreferredSize()
     val r = vp.getViewRect
     val tlSpan = timelineView.timeline.span
     val vSpan  = timelineView.span
     if( tlSpan.isEmpty || vSpan.isEmpty ) return
     val scale = dim.getWidth.toDouble / tlSpan.getLength
     r.x = ((vSpan.start - tlSpan.start) * scale + 0.5).toInt
     withViewPort( vp ) { vp.scrollRectToVisible( r )}
  }

  private def withViewPort( vp: JViewport )( thunk: => Unit ) {
      vp.removeChangeListener( viewPortListener )
      try { thunk }
      finally { vp.addChangeListener( viewPortListener )}
  }
*/
  private case class ViewportSelection( bounds: Rectangle, color: Color )
}
