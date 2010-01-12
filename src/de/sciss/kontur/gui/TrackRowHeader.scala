/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Color, GradientPaint, Graphics, Graphics2D, Paint }
import java.awt.event.{ MouseAdapter, MouseEvent, MouseListener }
import javax.swing.{ BorderFactory, JComponent, JLabel, JPanel, Spring, SpringLayout }
import scala.math._

import de.sciss.gui.{ GradientPanel }
import de.sciss.app.{ AbstractApplication, Application, DynamicAncestorAdapter,
                     DynamicListening, GraphicsHandler }
import de.sciss.util.{ Disposable }
import de.sciss.kontur.session.{ SessionElementSeq, Stake, Track }

trait TrackRowHeaderFactory {
	def createRowHeader[ T <: Stake ]( t: Track[ T ]) : TrackRowHeader
}

object DefaultTrackRowHeaderFactory
extends TrackRowHeaderFactory {
	def createRowHeader[ T <: Stake ]( t: Track[ T ]) : TrackRowHeader = {
      new DefaultTrackRowHeader
    }
}

/**
 *	A row header in Swing's table 'ideology'
 *	is a component left to the leftmost
 *	column of each row in a table. It serves
 *	as a kind of label for that specific row.
 *	This class shows a header left to each
 *	sound file's waveform display, with information
 *	about the channel index, possible selections
 *	and soloing/muting. In the future it could
 *	carry insert effects and the like.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.75, 22-Jul-08
 */
trait TrackRowHeader {
  def component: JComponent
}

object DefaultTrackRowHeader {
    private val colrSelected	= new Color( 0x00, 0x00, 0xFF, 0x2F )
    private val colrUnselected	= new Color( 0x00, 0x00, 0x00, 0x20 )
    private val colrDarken		= new Color( 0x00, 0x00, 0x00, 0x18 )
	private val pntSelected		= new GradientPaint(  0, 0, colrSelected, 36, 0,
                                                 new Color( colrSelected.getRGB() & 0xFFFFFF, true ))
	private val pntUnselected	= new GradientPaint(  0, 0, colrUnselected, 36, 0,
                                                  new Color( colrUnselected.getRGB() & 0xFFFFFF, true ))
	private val pntDarken		= new GradientPaint(  0, 0, colrDarken, 36, 0,
                                               new Color( colrDarken.getRGB() & 0xFFFFFF, true ))
}

class DefaultTrackRowHeader
extends JPanel
with TrackRowHeader with DynamicListening with Disposable {
  import DefaultTrackRowHeader._
  
	private val lbTrackName = new JLabel()

	protected var selected		= false

//	private final MapManager.Listener			trackListener;
//	private final SessionCollection.Listener	selectedTracksListener;

//	protected MutableSessionCollection.Editor	editor			= null;

	private var trackVar: Option[ Track[ _ ]] = None
	private var tracksVar: Option[ SessionElementSeq[ Track[ _ ]]] = None
//	protected MutableSessionCollection			selectedTracks	= null;

	private var	isListening	= false

    private val ml = new MouseAdapter() {
			/**
			 *	Handle mouse presses.
			 *	<pre>
			 *  Keyboard shortcuts as in ProTools:
			 *  Alt+Click   = Toggle item & set all others to same new state
			 *  Meta+Click  = Toggle item & set all others to opposite state
			 *	</pre>
			 *
			 *	@synchronization	attempts exclusive on TRNS + GRP
			 */
			override def mousePressed( e: MouseEvent ) {
				if( /* (editor == null) || */ (trackVar.isEmpty) ) return
                val t = trackVar.get

//				val id = editor.editBegin( this, getResourceString( "editTrackSelection" ))

				if( e.isAltDown ) {
                    t.selected = !selected // XXX EDIT
				} else if( e.isMetaDown ) {
                    tracksVar.foreach( _.foreach( t2 => t2.selected = !t2.selected )) // XXX EDIT
				} else if( e.isShiftDown ) {
                    t.selected = !selected // XXX EDIT
				} else if( !selected ) {
                    tracksVar.foreach( _.foreach( t2 => t2.selected = (t2 == t) )) // XXX EDIT
				}
//				editor.editEnd( id )
		    }
		}

    // ---- constructor ----
	{
		val lay	= new SpringLayout()
		setLayout( lay )

 		lbTrackName.setFont( AbstractApplication.getApplication().getGraphicsHandler().getFont(
            GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL ))
		val cons = lay.getConstraints( lbTrackName )
		cons.setX( Spring.constant( 7 ))
		cons.setY( Spring.minus( Spring.max(	// min( X, Y ) = -max( -X, -Y )
				Spring.constant( -4 ),
				Spring.minus( Spring.sum( Spring.sum( lay.getConstraint( SpringLayout.SOUTH, this ), Spring.minus( lay.getConstraint( SpringLayout.NORTH, this ))), Spring.constant( -15 ))))))
		add( lbTrackName )
		setBorder( BorderFactory.createMatteBorder( 0, 0, 0, 2, Color.white ))   // top left bottom right

		// --- Listener ---
        new DynamicAncestorAdapter( this ).addTo( this );

/*
		trackListener = new MapManager.Listener() {
			public void mapChanged( MapManager.Event e ) {
				trackMapChanged( e );
			}

			public void mapOwnerModified( MapManager.Event e )
			{
				if( e.getOwnerModType() == SessionObject.OWNER_RENAMED ) {
					checkTrackName();
				}
				trackChanged( e );
			}
		};
*/
	}

    def component: JComponent = this

	protected def getResourceString( key: String ) =
		AbstractApplication.getApplication().getResourceString( key )

    def track = trackVar
	def track_=( newTrack: Option[ Track[ _ ]]) {
		val wasListening = isListening
		stopListening
		trackVar = newTrack
		if( wasListening ) startListening
	}

    def tracks = tracksVar
    def tracks_=( newTracks: Option[ SessionElementSeq[ Track[ _ ]]]) {
		val wasListening = isListening
		stopListening
		tracksVar = newTracks
		if( wasListening ) startListening
    }

/*
	def setEditor( editor: MutableSessionCollection.Editor ) {
		if( this.editor != editor ) {
			this.editor = editor
			if( editor != null ) {
				this.addMouseListener( ml )
			} else {
				this.removeMouseListener( ml )
			}
		}
	}
*/
	def dispose {
		stopListening
	}

	/**
	 *  Determines if this row is selected
	 *  i.e. is part of the selected transmitters
	 *
	 *	@return	<code>true</code> if the row (and thus the transmitter) is selected
	 */
	def isSelected = selected

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

        val g2	= g.asInstanceOf[ Graphics2D ]
		val h	= getHeight
		val w	= getWidth
		val x	= min( w - 36, lbTrackName.getX + lbTrackName.getWidth )

	g2.translate( x, 0 )
		g2.setPaint( pntDarken )
		g2.fillRect( -x, 19, x + 36, 2 )
		g2.setPaint( if( selected ) pntSelected else pntUnselected )
		g2.fillRect( -x, 0, x + 36, 20 )
	g2.translate( -x, 0 )

	g2.translate( 0, h - 8 )
		g2.setPaint( GradientPanel.pntBottomBorder )
		g2.fillRect( 0, 0, w, 8 )
	g2.translate( 0, 8 - h )
	}

	override def paintChildren( g: Graphics ) {
		super.paintChildren( g )
		val g2 = g.asInstanceOf[ Graphics2D ]
		val w	= getWidth
		g2.setPaint( GradientPanel.pntTopBorder )
		g2.fillRect( 0, 0, w, 8 )
	}

	protected def checkTrackName {
        val name = trackVar.map( _.name ) getOrElse ""
		if( lbTrackName.getText != name ) {
			lbTrackName.setText( name )
		}
	}

// ---------------- DynamicListening interface ----------------

    private def trackListener( msg: AnyRef ) : Unit = msg match {
      case Track.SelectionChanged( _, newState ) => {
          selected = newState
          repaint()
      }
      case _ =>
    }

    def startListening {
    	if( !isListening ) {
    		isListening = true
            trackVar.foreach( _.addListener( trackListener ))
    		checkTrackName
    		if( selected != (trackVar.map( _.selected ) getOrElse false) ) {
    			selected = !selected
    			repaint()
        	}
    	}
    }

    def stopListening {
    	if( isListening ) {
    		isListening = false
            trackVar.foreach( _.removeListener( trackListener ))
    	}
    }
}