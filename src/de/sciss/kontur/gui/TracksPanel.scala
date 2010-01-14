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
class TracksPanel( val timelinePanel: TimelinePanel, val mainView: JComponent )
extends JPanel( new BorderLayout() )
with TracksTable {
    private val tracksView        = timelinePanel.tracksView
    private val scroll            = new TimelineScroll( timelinePanel.timelineView )
	private val allHeaderPanel    = new JPanel( new BorderLayout() )
	private val trackHeaderPanel  = new JPanel( new StretchedGridLayout( 0, 1, 1, 1 ))
//    private val markerTrackHeader = new DefaultTrackRowHeader()
    private var trhf: TrackRowHeaderFactory = DefaultTrackRowHeaderFactory

//	private var collTrackHeaders  = Queue[ TrackRowHeader ]()
	private var mapTrackHeaders   = Map[ Track, TrackRowHeader ]()
    private val tracks = tracksView.tracks

  	private def tracksViewListener( msg: AnyRef ) : Unit = {
println(" TracksPanel : tracksViewListener " + msg )
      msg match {
      case tracks.ElementAdded( idx, t ) => addTrack( idx, t )
      case tracks.ElementRemoved( idx, t ) => removeTrack( idx, t )
    }}

    // ---- constructor ----
    {
//		val markerAxis	= timelinePanel.markerAxis

//		markerTrackHeader.setPreferredSize( new Dimension( 63, markerAxis.getPreferredSize().height ))	// XXX
//		markerTrackHeader.setMaximumSize( new Dimension( 128, markerAxis.getMaximumSize().height ))		// XXX

//		if( !markerAxis.isVisible() ) markerTrackHeader.setVisible( false )
        val topBox  = Box.createVerticalBox()
        val gp      = GUIUtil.createGradientPanel()
        gp.setBottomBorder( true )
        gp.setLayout( null )
        gp.setPreferredSize( new Dimension( 64, timelinePanel.timelineAxis.getPreferredSize().height ))
        topBox.add( gp )
//        topBox.add( markerTrackHeader )
        allHeaderPanel.add( topBox, BorderLayout.NORTH )
        allHeaderPanel.add( trackHeaderPanel, BorderLayout.CENTER )

        add( timelinePanel, BorderLayout.CENTER )
		add( allHeaderPanel, BorderLayout.WEST )
        val southBox = Box.createHorizontalBox()
		southBox.add( scroll )

        val intruding = true // XXX System.getProperty( )
        if( intruding ) southBox.add( Box.createHorizontalStrut( 16 ))
        add( southBox, BorderLayout.SOUTH )

		timelinePanel.tracksTable = Some( this )
        tracksView.addListener( tracksViewListener )
        
        { var i = 0; tracks.foreach( t => { addTrack( i, t ); i += 1 })}
	}
/*
	private var tracksVar: Option[ SessionElementSeq[ Track[ _ ]]] = None
    def tracks = tracksVar
	def tracks_=( newTracks: Option[ SessionElementSeq[ Track[ _ ]]]) {
        tracksVar.foreach( tt => {
            tt.removeListener( tracksListener( tt ))
            tt.foreach( t => removeTrack( 0, t )) // idx always zero because we remove from the beginning
        })
        tracksVar = newTracks
//		checkSetMarkTrack
        newTracks.foreach( tt => {
            tt.addListener( tracksListener( tt ))
            var i = 0
            tt.foreach( t => { addTrack( i, t ); i += 1 })
        })
		timelinePanel.tracks = tracksVar
   	}
*/
    def trackRowHeaderFactory = trhf
	def trackRowHeaderFactory_=( newTRHF: TrackRowHeaderFactory ) {
		trhf = newTRHF
	}

	def dispose {
      tracksView.removeListener( tracksViewListener )
//      tracks = None
	}

//	private def checkSetMarkTrack {
//      markerTrackHeader.track   = markerTrack
//      markerTrackHeader.tracks  = tracksVar
//      if( markerTrack.isDefined ) {
//          mapTrackHeaders += (markerTrack.get -> markerTrackHeader)
//      } else {
//          println( "marker track removal NOT YET IMPLEMENTED" )
//      }
//	}

    private def addTrack( idx: Int, t: Track ) {
      val trackRowHead = trhf.createRowHeader( t, tracksView )
      mapTrackHeaders += (t -> trackRowHead)
      trackHeaderPanel.add( trackRowHead.component, idx )
      revalidate(); repaint()
    }

    private def removeTrack( idx: Int, t: Track ) {
      val trackRowHead = mapTrackHeaders( t )
      assert( trackHeaderPanel.getComponent( idx ) == trackRowHead )
      mapTrackHeaders -= t
      trackHeaderPanel.remove( idx )
      // we could dispose the header if dispose was defined,
      // but we rely on dynamiclistening instead:
      // trackRowHead.dispose

      // XXX eventually could check if header was visible
      // (if not we do not need to revalidate)
      revalidate(); repaint()
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
/*
    private var markerTrackVar: Option[ Track ] = None
    def markerTrack = markerTrackVar
	def markerTrack_=( t: Option[ Track ]) {
        markerTrackVar = t
//		checkSetMarkTrack
		timelinePanel.markerTrack = markerTrackVar
	}

    private var mainViewVar: Option[ JComponent ] = None

    def mainView = mainViewVar
	def mainView_=( view: Option[ JComponent ]) {
        mainViewVar.foreach( mv => timelinePanel.remove( mv ))
		mainViewVar = view
        mainViewVar.foreach( mv => timelinePanel.add( mv ))
	}
*/

// ------------------ TracksTable interface ------------------

//  	def mainView: JComponent
	def getRowHeader( t: Track ) : TrackRowHeader = mapTrackHeaders( t )
    def numTracks: Int = tracks.size
    def getTrack( idx: Int ) : Option[ Track ] = tracks.get( idx )
	def indexOf( t: Track ) : Int =  tracks.indexOf( t )

	def getTrackBounds( t: Track ) : Rectangle = {
//        val res = new Rectangle()
        val trh = mapTrackHeaders.get( t ) getOrElse (throw new IllegalArgumentException( t.toString ))
		val res = trh.component.getBounds() // ( res )
		res.x = 0
        res.width = mainView.getWidth
		res
	}
}
