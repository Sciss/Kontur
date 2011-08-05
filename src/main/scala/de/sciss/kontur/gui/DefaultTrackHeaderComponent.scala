/*
 *  TrackHeaderComponent.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

import java.awt.{ Color, GradientPaint, Graphics, Graphics2D }
import java.awt.dnd.{ DnDConstants, DropTarget, DropTargetAdapter,
                     DropTargetDragEvent, DropTargetDropEvent }
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.{ BorderFactory, JLabel, JPanel, Spring, SpringLayout }

import de.sciss.gui.GradientPanel
import de.sciss.app.{ AbstractApplication, DynamicAncestorAdapter,
                     DynamicListening, GraphicsHandler }
import de.sciss.util.Disposable
import de.sciss.kontur.session.{ AudioTrack, Diffusion, Renamable, Track }
import de.sciss.synth.Model

//import Track.Tr

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
object DefaultTrackHeaderComponent {
    private val colrSelected	= new Color( 0x00, 0x00, 0xFF, 0x2F )
    private val colrUnselected	= new Color( 0x00, 0x00, 0x00, 0x20 )
    private val colrDarken		= new Color( 0x00, 0x00, 0x00, 0x18 )
	private val pntSelected		= new GradientPaint(  0, 0, colrSelected, 36, 0,
                                                 new Color( colrSelected.getRGB & 0xFFFFFF, true ))
	private val pntUnselected	= new GradientPaint(  0, 0, colrUnselected, 36, 0,
                                                  new Color( colrUnselected.getRGB & 0xFFFFFF, true ))
	private val pntDarken		= new GradientPaint(  0, 0, colrDarken, 36, 0,
                                               new Color( colrDarken.getRGB & 0xFFFFFF, true ))
}

class DefaultTrackHeaderComponent( protected val track: Track, trackList: TrackList )
extends JPanel
with DynamicListening with Disposable {
  import DefaultTrackHeaderComponent._
  
	private val lbTrackName = new JLabel()

    protected lazy val trackListElement = trackList.getElement( track ).get
//    protected val trailView = trackListElement.trailView.asInstanceOf[ TrailView[ track.T ]]

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
//				val id = editor.editBegin( this, getResourceString( "editTrackSelection" ))
                trackList.editor.foreach( ed => {
                    val ce = ed.editBegin( "trackSelection" )
                    val thisSel = trackListElement.selected
    				if( e.isShiftDown ) { // toggle single
                      if( thisSel ) {
                        ed.editDeselect( ce, trackListElement )
                      } else {
                        ed.editSelect( ce, trackListElement )
                      }
            		} else if( e.isMetaDown ) { // toggle single, complement reset
                      val collRest = trackList.filter( _ != trackListElement )
                      if( thisSel ) {
                        ed.editDeselect( ce, trackListElement )
                        ed.editSelect( ce, collRest: _* )
                      } else {
                        ed.editSelect( ce, trackListElement )
                        ed.editDeselect( ce, collRest: _* )
                      }
                	} else if( e.isAltDown ) { // toggle single and align reset
                      if( thisSel ) {
                        ed.editDeselect( ce, trackList.toList: _* )
                      } else {
                        ed.editSelect( ce, trackList.toList: _* )
                      }
    				} else if( !thisSel ) { // select single, deselect reset
                        val collRest = trackList.filter( _ != trackListElement )
                        ed.editSelect( ce, trackListElement )
                        ed.editDeselect( ce, collRest: _* )
        			}
      				ed.editEnd( ce )
                })
		    }
		}

    // ---- constructor ----
	{

		val lay	= new SpringLayout()
		setLayout( lay )
//        setPreferredSize( new Dimension( 64, 64 )) // XXX

 		lbTrackName.setFont( AbstractApplication.getApplication.getGraphicsHandler.getFont(
            GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL ))
		val cons = lay.getConstraints( lbTrackName )
		cons.setX( Spring.constant( 7 ))
		cons.setY( Spring.minus( Spring.max(	// min( X, Y ) = -max( -X, -Y )
				Spring.constant( -4 ),
				Spring.minus( Spring.sum( Spring.sum( lay.getConstraint( SpringLayout.SOUTH, this ), Spring.minus( lay.getConstraint( SpringLayout.NORTH, this ))), Spring.constant( -15 ))))))
		add( lbTrackName )

        val cons2 = lay.getConstraints( this )
        cons2.setWidth( Spring.constant( 64 ))
        cons2.setHeight( Spring.constant( 64 ))

		setBorder( BorderFactory.createMatteBorder( 0, 0, 0, 2, Color.white ))   // top left bottom right

		// --- Listener ---
      addMouseListener( ml )
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

//    def component: JComponent = this

	protected def getResourceString( key: String ) =
		AbstractApplication.getApplication.getResourceString( key )

/*
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
*/

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
	def dispose() {
		stopListening()
	}

	/**
	 *  Determines if this row is selected
	 *  i.e. is part of the selected transmitters
	 *
	 *	@return	<code>true</code> if the row (and thus the transmitter) is selected
	 */
//	def isSelected = selected

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

      val g2   = g.asInstanceOf[ Graphics2D ]
		val h	   = getHeight
		val w	   = getWidth
		val x	   = math.min( w - 36, lbTrackName.getX + lbTrackName.getWidth )

	g2.translate( x, 0 )
		g2.setPaint( pntDarken )
		g2.fillRect( -x, 19, x + 36, 2 )
		g2.setPaint( if( trackListElement.selected ) pntSelected else pntUnselected )
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

	private def checkTrackName() {
		if( lbTrackName.getText != track.name ) {
			lbTrackName.setText( track.name )
		}
	}

// ---------------- DynamicListening interface ----------------

    private val trackListListener: Model.Listener = {
      case TrackList.SelectionChanged( mod @ _* ) => {
          if( mod.contains( trackListElement )) repaint()
      }
      case Renamable.NameChanged( _, newName ) => checkTrackName()
    }

    def startListening() {
    	if( !isListening ) {
    		isListening = true
            trackList.addListener( trackListListener )
    		checkTrackName()
    	}
    }

    def stopListening() {
    	if( isListening ) {
    		isListening = false
            trackList.removeListener( trackListListener )
    	}
    }
}

class AudioTrackHeaderComponent( audioTrack: AudioTrack, trackList: TrackList )
extends DefaultTrackHeaderComponent( audioTrack, trackList ) {

    // ---- constructor ----
    {
        new DropTarget( this, DnDConstants.ACTION_LINK, new DropTargetAdapter {
           private def process( dtde: DropTargetDragEvent ) {
              if( dtde.isDataFlavorSupported( Diffusion.flavor )) {
                  dtde.acceptDrag( DnDConstants.ACTION_LINK )
              } else {
                  dtde.rejectDrag()
              }
           }

           override def dragEnter( dtde: DropTargetDragEvent ) { process( dtde )}
           override def dragOver( dtde: DropTargetDragEvent ) { process( dtde )}

           def drop( dtde: DropTargetDropEvent ) {
             if( dtde.isDataFlavorSupported( Diffusion.flavor )) {
                 dtde.acceptDrop( DnDConstants.ACTION_LINK )
                 dtde.getTransferable.getTransferData( Diffusion.flavor ) match {
                    case diff: Diffusion => {
                       val ce = audioTrack.editBegin( "editTrackDiffusion" )
                       audioTrack.editDiffusion( ce, Some( diff ))
                       audioTrack.editEnd( ce )
                       dtde.dropComplete( true )
                    }
                    case _ =>
                 }
             } else {
                 dtde.rejectDrop()
             }
           }
        })
      }
}