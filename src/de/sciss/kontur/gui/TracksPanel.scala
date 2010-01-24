/*
 *  TracksPanel.scala
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

import java.awt.{ BorderLayout, Dimension, Rectangle }
import javax.swing.{ Box, JComponent, JPanel, JScrollPane, ScrollPaneConstants }
import ScrollPaneConstants._
import scala.collection.immutable.{ Queue }

import de.sciss.gui.{ GUIUtil, GradientPanel, StretchedGridLayout }
import de.sciss.kontur.session.{ Marker, Session, SessionElementSeq, Stake, Timeline, Track }
import de.sciss.kontur.util.{ Model }

//import Track.Tr

/**
 *	@author		Hanns Holger Rutz
 * 	@version	0.12, 11-Jan-10
 */
class TracksPanel( doc: Session, val tracksView: TracksView,
                   val timelinePanel: TimelinePanel )
extends JScrollPane( VERTICAL_SCROLLBAR_ALWAYS,
                     HORIZONTAL_SCROLLBAR_ALWAYS ) // JPanel( new BorderLayout() )
with TracksTable with Model {
//    private val tracksView        = timelinePanel.tracksView
//    private val scroll            = new TimelineScroll( timelineView )
//	private val allHeaderPanel    = new JPanel( new BorderLayout() )
//	private val trackHeaderPanel  = new JPanel( new StretchedGridLayout( 0, 1, 1, 1 ))

    val columnHeaderView  = Box.createVerticalBox()
    private val rowHeaderView     = Box.createVerticalBox()

//    private val markerTrackHeader = new DefaultTrackRowHeader()
    private var trf: TrackRendererFactory = DefaultTrackRendererFactory

//	private var collTrackHeaders  = Queue[ TrackRowHeader ]()
//	private var mapTrackRenderers   = Map[ Track[ T <: Stake[ T ]], TrackRenderer ]()
	private var mapTrackRenderers   = Map[ Track[ _ <: Stake[ _ ]], TrackRenderer ]()
    private val tracks = tracksView.tracks
    private val timelineView = timelinePanel.timelineView
	private val timelineAxis = new TimelineAxis( timelineView, None ) // Some( this )
    private val viewPort = new TimelineViewport( timelineView )

  	private val tracksViewListener = (msg: AnyRef) => {
//println( "TracksPanel : tracksViewListener " + msg )
      msg match {
          case tracks.ElementAdded( idx, t ) => addTrack( idx, t )
          case tracks.ElementRemoved( idx, t ) => removeTrack( idx, t )
          case _ => // println( "nadisima" )
      }
      dispatch( msg )
    }

    // ---- constructor ----
    {
//		val markerAxis	= timelinePanel.markerAxis

//		markerTrackHeader.setPreferredSize( new Dimension( 63, markerAxis.getPreferredSize().height ))	// XXX
//		markerTrackHeader.setMaximumSize( new Dimension( 128, markerAxis.getMaximumSize().height ))		// XXX

//		if( !markerAxis.isVisible() ) markerTrackHeader.setVisible( false )
//        val topBox  = Box.createVerticalBox()
        val gp      = GUIUtil.createGradientPanel()
        gp.setBottomBorder( true )
        gp.setLayout( null )
//        gp.setPreferredSize( new Dimension( 64, timelineAxis.getPreferredSize().height ))
//        topBox.add( gp )
        setBorder( null )
        setViewport( viewPort )
        viewPort.setBorder( null )
        setCorner( UPPER_LEFT_CORNER, gp )
//
//        topBox.add( markerTrackHeader )
//        allHeaderPanel.add( topBox, BorderLayout.NORTH )
//        allHeaderPanel.add( trackHeaderPanel, BorderLayout.CENTER )

  //      add( timelinePanel, BorderLayout.CENTER )
        setViewportView( timelinePanel )

//		add( allHeaderPanel, BorderLayout.WEST )
        setRowHeaderView( rowHeaderView )
        columnHeaderView.add( timelineAxis )
//columnHeaderView.setBackground( java.awt.Color.blue )
        setColumnHeaderView( columnHeaderView )
        timelineAxis.viewPort = Some( getColumnHeader )

//        val southBox = Box.createHorizontalBox()
//		southBox.add( scroll )

//        val intruding = true // XXX System.getProperty( )
//        if( intruding ) southBox.add( Box.createHorizontalStrut( 16 ))
//        add( southBox, BorderLayout.SOUTH )

		timelinePanel.tracksTable = Some( this )
        tracksView.addListener( tracksViewListener )
//		timelineView.addListener( timelineListener )
        
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
    def trackRendererFactory = trf
	def trackRendererFactory_=( newTRF: TrackRendererFactory ) {
		trf = newTRF
	}

	def dispose {
      tracksView.removeListener( tracksViewListener )
//      tracks = None
	}

//	private def checkSetMarkTrack {
//      markerTrackHeader.track   = markerTrack
//      markerTrackHeader.tracks  = tracksVar
//      if( markerTrack.isDefined ) {
//          mapTrackRenderers += (markerTrack.get -> markerTrackHeader)
//      } else {
//          println( "marker track removal NOT YET IMPLEMENTED" )
//      }
//	}

    private def addTrack( idx: Int, t: Track[ _ <: Stake[ _ ]]) {
      val tr = trf.createTrackRenderer( doc, t, tracksView, timelineView )
//      val trackRowHead = trf.createRowHeader( t, tracksView )
      mapTrackRenderers += (t -> tr)
      rowHeaderView.add( tr.trackHeaderComponent, idx )
      timelinePanel.add( tr.trackComponent, idx )
//      revalidate(); repaint()
      rowHeaderView.revalidate()
//println( "ROW HEADER PREF " + rowHeaderView.getPreferredSize() )
      timelinePanel.revalidate()
    }

    private def removeTrack( idx: Int, t: Track[ _ <: Stake[ _ ]]) {
//      val tr = mapTrackRenderers( t )
//      assert( trackHeaderPanel.getComponent( idx ) == trackRowHead )
      mapTrackRenderers -= t
      rowHeaderView.remove( idx )
      timelinePanel.remove( idx )
      // we could dispose the header if dispose was defined,
      // but we rely on dynamiclistening instead:
      // trackRowHead.dispose

      // XXX eventually could check if header was visible
      // (if not we do not need to revalidate)
//      revalidate(); repaint()
      rowHeaderView.revalidate()
      timelinePanel.revalidate()
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
				mapTrackRenderers.remove( t )
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

            val trackRowHead = trf.createRowHeader( t )
			trackRowHead.setTrack( t, activeTracks, selectedTracks )
			trackRowHead.setEditor( tracksEditor )
			collTrackHeaders.enqueue( trackRowHead )
			mapTrackRenderers.put( t, trackRowHead )
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
//	private def updateOverviews( allTracks: Boolean ) {
//		if( allTracks ) timelinePanel.updateAll
//	}
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
	def getTrackRenderer[ T <: Stake[ T ]]( t: Track[ T ]) : TrackRenderer = mapTrackRenderers( t )
    def numTracks: Int = tracks.size
    def getTrack( idx: Int ) : Option[ Track[ _ <: Stake[ _ ]]] = tracks.get( idx )
	def indexOf[ T <: Stake[ T ]]( t: Track[ T ]) : Int =  tracks.indexOf( t )

	def getTrackBounds[ T <: Stake[ T ]]( t: Track[ T ]) : Rectangle = {
//        val res = new Rectangle()
        val tr = mapTrackRenderers.get( t ) getOrElse (throw new IllegalArgumentException( t.toString ))
		val res = tr.trackComponent.getBounds() // ( res )
//		res.x = 0
//        res.width = mainView.getWidth
		res
	}
}
