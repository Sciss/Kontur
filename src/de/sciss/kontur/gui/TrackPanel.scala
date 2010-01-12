/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ BorderLayout, Dimension, Rectangle }
import javax.swing.{ Box, JComponent, JPanel }
import scala.collection.immutable.{ Queue }

import de.sciss.gui.{ GUIUtil, GradientPanel, StretchedGridLayout }
import de.sciss.kontur.session.{ Marker, SessionElementSeq, Track }

/**
 *	@author		Hanns Holger Rutz
 * 	@version	0.12, 11-Jan-10
 */
class TrackPanel( val timelinePanel: TimelinePanel )
extends JPanel( new BorderLayout() )
with TrackTable {
    private val scroll            = new TimelineScroll( timelinePanel.timelineView )
	private val allHeaderPanel    = new JPanel( new BorderLayout() )
	private val trackHeaderPanel  = new JPanel( new StretchedGridLayout( 0, 1, 1, 1 ))
    private val markerTrackHeader = new DefaultTrackRowHeader()
    private var trhf: TrackRowHeaderFactory = DefaultTrackRowHeaderFactory

	private var collTrackHeaders  = Queue[ TrackRowHeader ]()
	private var mapTrackHeaders   = Map[ Track[ _ ], TrackRowHeader ]()

  	private def tracksListener( seq: SessionElementSeq[ Track[ _ ]])( msg: AnyRef ) : Unit = msg match {
      case seq.ElementAdded( idx, t ) => // documentUpdate // XXX
      case seq.ElementRemoved( idx, t ) => // documentUpdate // XXX
    }

    // ---- constructor ----
    {
		val markerAxis	= timelinePanel.markerAxis

		markerTrackHeader.setPreferredSize( new Dimension( 63, markerAxis.getPreferredSize().height ))	// XXX
		markerTrackHeader.setMaximumSize( new Dimension( 128, markerAxis.getMaximumSize().height ))		// XXX

		if( !markerAxis.isVisible() ) markerTrackHeader.setVisible( false )
        val topBox  = Box.createVerticalBox()
        val gp      = GUIUtil.createGradientPanel()
        gp.setBottomBorder( true )
        gp.setLayout( null )
        gp.setPreferredSize( new Dimension( 0, timelinePanel.timelineAxis.getPreferredSize().height ))
        topBox.add( gp )
        topBox.add( markerTrackHeader )
        allHeaderPanel.add( topBox, BorderLayout.NORTH )
        allHeaderPanel.add( trackHeaderPanel, BorderLayout.CENTER )

        add( timelinePanel, BorderLayout.CENTER )
		add( allHeaderPanel, BorderLayout.WEST )
        val southBox = Box.createHorizontalBox()
		southBox.add( scroll )

        val intruding = true // XXX System.getProperty( )
        if( intruding ) southBox.add( Box.createHorizontalStrut( 16 ))
        add( southBox, BorderLayout.SOUTH )

		timelinePanel.trackTable = Some( this )
	}

	private var tracksVar: Option[ SessionElementSeq[ Track[ _ ]]] = None
    def tracks = tracksVar
	def tracks_=( newTracks: Option[ SessionElementSeq[ Track[ _ ]]]) {
        tracksVar.foreach( t => t.removeListener( tracksListener( t )))
        tracksVar = newTracks
		checkSetMarkTrack
        newTracks.foreach( t => t.addListener( tracksListener( t )))
		timelinePanel.tracks = tracksVar
   	}

    def trackRowHeaderFactory = trhf
	def trackRowHeaderFactory_=( newTRHF: TrackRowHeaderFactory ) {
		trhf = newTRHF
	}

	def dispose {
      tracks = None
	}

	private def checkSetMarkTrack {
      markerTrackHeader.track   = markerTrack
      markerTrackHeader.tracks  = tracksVar
      if( markerTrack.isDefined ) {
          collTrackHeaders.enqueue( markerTrack )
          mapTrackHeaders += (markerTrack.get -> markerTrackHeader)
      } else {
//          collTrackHeaders.filter( _ != markerTrack )
          println( "marker track removal NOT YET IMPLEMENTED" )
      }
	}

/*
	protected def documentUpdate {
		var reval = false

		val newNumWaveTracks	= activeTracks.size // EEE doc.getDisplayDescr().channels;
		val oldNumWaveTracks	= collTrackHeaders.size

		// first kick out editors whose tracks have been removed
		for( ch <- 0 until oldNumWaveTracks ) {
			val trackRowHead  = collTrackHeaders.get( ch );
			val t             = trackRowHead.getTrack();

			if( !activeTracks.contains( t )) {
				trackRowHead	= collTrackHeaders.remove( ch )
				mapTrackHeaders.remove( t )
				oldNumWaveTracks -= 1
				// XXX : dispose trnsEdit (e.g. free vectors, remove listeners!!)
				trackHeaderPanel.remove( trackRowHead )
				ch -= 1
				trackRowHead.dispose
				reval = true
			}
		}
		// next look for newly added transmitters and create editors for them

// EEE
//newLp:
         for( ch <- 0 until newNumWaveTracks ) {

			val t = activeTracks.get( ch )
			for( ch2 <- 0 until oldNumWaveTracks ) {
				val trackRowHead = collTrackHeaders.get( ch2 )
				if( trackRowHead.track == Some( t )) continue newLp
			}

            val trackRowHead = trhf.createRowHeader( t )
			trackRowHead.setTrack( t, activeTracks, selectedTracks )
			trackRowHead.setEditor( tracksEditor )
			collTrackHeaders.enqueue( trackRowHead )
			mapTrackHeaders.put( t, trackRowHead )
			trackHeaderPanel.add( trackRowHead, ch - 1 ) // XXX tricky! -1 because of marker track

			reval = true
		}

		if( reval ) {
			revalidate()
			repaint()
		}
		updateOverviews( /* false, */ true )
	}
*/

//		if( tracksEditor != editor ) {
//			tracksEditor = editor;
//			for( int i = 0; i < collTrackHeaders.size(); i++ ) {
//				final TrackRowHeader trh = collTrackHeaders.get( i );
//				trh.setEditor( tracksEditor );
//			}
//			markTrackHeader.setEditor( tracksEditor );
//		}
//	}

	// sync: attempts exclusive on MTE and shared on TIME!
	private def updateOverviews( allTracks: Boolean ) {
//		waveView.update( timelineVis );
		if( allTracks ) timelinePanel.updateAll
	}

    private var markerTrackVar: Option[ Track[ Marker ]] = None
    def markerTrack = markerTrackVar
	def markerTrack_=( t: Option[ Track[ Marker ]]) {
        markerTrackVar = t
		checkSetMarkTrack
		timelinePanel.markerTrack = markerTrackVar
	}

    private var mainViewVar: Option[ JComponent ] = None

    def mainView = mainViewVar
	def mainView_=( view: Option[ JComponent ]) {
        mainViewVar.foreach( mv => timelinePanel.remove( mv ))
		mainViewVar = view
        mainViewVar.foreach( mv => timelinePanel.add( mv ))
	}

// ------------------ TracksTable interface ------------------

//  	def mainView: JComponent
	def getRowHeader( t: Track[ _ ]) : TrackRowHeader = mapTrackHeaders( t )

    def numTracks: Int = tracks.size

    def getTrack( idx: Int ) : Option[ Track[ _ ]] = {
      if( tracks.isDefined ) {
        tracks.get.get( idx )
      } else None
    }

	def indexOf( t: Track[ _ ]) : Int = {
      if( tracks.isDefined ) {
        tracks.get.indexOf( t )
      } else -1
    }

	def getTrackBounds( t: Track[ _ ], res: Rectangle = new Rectangle() ) : Rectangle = {
        val trh = mapTrackHeaders.get( t ) getOrElse (throw new IllegalArgumentException( t.toString ))
		trh.component.getBounds( res )
		res.x = 0
		if( mainView.isDefined ) {
            val v = mainView.get
			res.width = v.getWidth
//			if( Some( t ) == markerTrack ) {	// XXX stupid special handling
//				res.y -= v.getY
//			}
		} else {
			res.width = 0
		}
		res
	}
}
