/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Color, Graphics2D, Rectangle }
import java.awt.event.{ ActionEvent, ActionListener }
import javax.swing.{ BoxLayout, JComponent, Timer }
import scala.math._

import de.sciss.gui.{ ComponentHost, TopPainter }
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ Marker, SessionElementSeq, Timeline,
                                Track, Trail }

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.14, 09-Jan-10
 */
//object TimelinePanel {
//}

class TimelinePanel( val tracksView: TracksView, val timelineView: TimelineView )
extends ComponentHost
with TopPainter {
	private val colrSelection			= new Color( 0x00, 0x00, 0xFF, 0x2F ); // GraphicsUtil.colrSelection;
	private val colrPosition			= new Color( 0xFF, 0x00, 0x00, 0x7F );
	private val colrSelection2			= new Color( 0x00, 0x00, 0x00, 0x20 );  // selected timeline span over unselected trns
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
    private val tracks = tracksView.tracks

//	private final Timer							playTimer;

	// !!! for some crazy reason, these need to be volatile because otherwise
	// the playTimer's actionPerformed body might use a cached value !!!
	// how can this happen when javax.swing.Timer is playing on the event thread?!
//	protected double							playRate				= 1.0;
//	protected long								playStartPos			= 0;
//	protected long								playStartTime;
//	protected boolean							isPlaying				= false;

	val timelineAxis = new TimelineAxis( timelineView, this )
//	val markerAxis = new MarkerAxis( timelineView, this )
	private var markerTrail: Option[ Trail[ Marker ]] = None

	protected val markVisible = true

	private var tracksTableVar: Option[ TracksTable ] = None

   // ---- constructor ----
	{
		addTopPainter( this )
		setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ))

		add( timelineAxis )
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
        tracksView.addListener( tracksViewListener )
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

    private def tracksViewListener( msg: AnyRef ) : Unit = {
println(" TimelinePanel : tracksViewListener " + msg )
      msg match {
      case tracks.ElementAdded( idx, elem ) => updateSelectionAndRepaint
      case tracks.ElementRemoved( idx, elem ) => updateSelectionAndRepaint
      case TracksView.SelectionChanged( _ ) => updateSelectionAndRepaint
    }}

    private def markerListener( msg: AnyRef ) : Unit = msg match {
      case Trail.Changed( span ) => repaintMarkers( span )
    }

    def tracksTable = tracksTableVar
    def tracksTable_=( newTT: Option[ TracksTable ]) {
      tracksTableVar = newTT
    }
/*
	private var tracksVar: Option[ SessionElementSeq[ Track[ _ ]]] = None
    def tracks = tracksVar
	def tracks_=( newTracks: Option[ SessionElementSeq[ Track[ _ ]]]) {
        tracksVar.foreach( t => t.removeListener( tracksListener( t )))
        tracksVar = newTracks
        newTracks.foreach( t => t.addListener( tracksListener( t )))
   	}
*/

/*
    private var markerTrackVar: Option[ Track ] = None
    def markerTrack = markerTrackVar
	def markerTrack_=( t: Option[ Track ]) {
      markerTrail.foreach( _.removeListener( markerListener ))
      markerTrail = t.map( _.trail )
      markerTrail.foreach( _.addListener( markerListener ))
      markerAxis.trail = markerTrail
      markerTrackVar = t
	}
*/

//	public void addCatchBypass() { /* scroll.addCatchBypass(); XXX*/ }
//	public void removeCatchBypass() { /* scroll.removeCatchBypass(); XXX*/ }

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

	def paintOnTop( g2: Graphics2D ) {
		val r = new Rectangle( 0, 0, getWidth(), getHeight() ) // getViewRect();
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
	override def dispose {
		timelineView.removeListener( timelineListener )
        tracksView.removeListener( tracksViewListener )
//		markerTrack = None
//		this.stop
		super.dispose
	}

	private def recalcTransforms( newRect: Rectangle ) {
		vpRecentRect = newRect // getViewRect();

		if( !timelineVis.isEmpty ) {
			vpScale = (vpRecentRect.width.toDouble / max( 1, timelineVis.getLength )).toFloat
//			playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
			vpPosition		= ((timelinePos - timelineVis.start) * vpScale + 0.5f).toInt
			vpPositionRect.setBounds( vpPosition, 0, 1, vpRecentRect.height )
			if( !timelineSel.isEmpty ) {
				val x			= ((timelineSel.start - timelineVis.start) * vpScale + 0.5f).toInt + vpRecentRect.x
				val w			= max( 1, ((timelineSel.stop - timelineVis.start) * vpScale + 0.5f).toInt - x )
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

		if( vpScale > 0f ) {
			vpPosition	= ((timelinePos - timelineVis.start) * vpScale + 0.5f).toInt
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

        tracksTableVar.foreach( tt => {
          val v = tt.mainView
            val x	= v.getX
            val y	= v.getY
            vpSelections ::= ViewportSelection( timelineAxis.getBounds, colrSelection )

            tracks.foreach( t => {
                val r	= tt.getTrackBounds( t )
                r.translate( x, y )
                vpSelections ::= ViewportSelection( r,
                   if( tracksView.isSelected( t )) colrSelection else colrSelection2 )
              })
         })
    }

	// ---------------- TimelineListener interface ----------------

    private def timelineListener( msg: AnyRef ): Unit = msg match {
      case TimelineCursor.PositionChanged( _, newPos ) => {
//println( "POSITION CHANGED " + oldPos + " -> " + newPos )
		timelinePos = newPos
		updatePositionAndRepaint
      }
      case TimelineView.SpanChanged( _, newSpan ) => {
    	timelineVis	= newSpan
		updateTransformsAndRepaint( false )
      }
      case TimelineSelection.SpanChanged( oldSpan, newSpan ) => {
   		timelineSel	= newSpan
		updateSelectionAndRepaint
		if( oldSpan.isEmpty != newSpan.isEmpty ) {
//			updateEditEnabled( !newSpan.isEmpty )
		}
      }
      case Timeline.RateChanged( _, newRate ) => {
		timelineRate = newRate
//		playTimer.setDelay( Math.min( (int) (1000 / (vpScale * timelineRate * Math.abs( playRate ))), 33 ));
      }
    }

  private case class ViewportSelection( bounds: Rectangle, color: Color )
}
