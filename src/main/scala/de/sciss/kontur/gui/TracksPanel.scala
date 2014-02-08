/*
 *  TracksPanel.scala
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

import java.awt.Color
import javax.swing.{ Box, JPanel, JScrollPane, ScrollPaneConstants }
import ScrollPaneConstants._

import session.Session
import util.Model

class TracksPanel( val doc: Session, val timelinePanel: TimelinePanel )
extends JScrollPane( VERTICAL_SCROLLBAR_ALWAYS,
                     HORIZONTAL_SCROLLBAR_ALWAYS ) // JPanel( new BorderLayout() )
with BasicTrackList with TrackToolsListener {
    // note: timelineView is accessed in the constructor of BasicTrackList
    lazy val timelineView         = timelinePanel.timelineView

    val columnHeaderView          = Box.createVerticalBox()
    private val rowHeaderView     = Box.createVerticalBox()

    private val timelineAxis      = new TimelineAxis( timelineView, None ) // Some( this )
    private val viewPort          = new TimelineViewport( timelineView )
    private var trackTools: Option[ TrackTools ] = None

    private val trackListListener: Model.Listener = {
       case TrackList.ElementAdded(   idx: Int, e: TrackListElement ) => addTrack( idx, e )
       case TrackList.ElementRemoved( idx: Int, e: TrackListElement ) => removeTrack( idx, e )
    }

    // ---- constructor ----
    {
//        val gp      = GUIUtil.createGradientPanel()
//        gp.setBottomBorder( true )
//        gp.setLayout( null )
        setBorder( null )
        viewPort.setBackground( new Color( 0x28, 0x28, 0x28 ))
        setViewport( viewPort )
        viewPort.setBorder( null )
//        setCorner( UPPER_LEFT_CORNER, gp )
        setCorner( UPPER_LEFT_CORNER, new JPanel() ) // fixes white background problem
        setCorner( LOWER_LEFT_CORNER, new JPanel() ) // fixes white background problem
        setCorner( UPPER_RIGHT_CORNER, new JPanel() ) // fixes white background problem
        setViewportView( timelinePanel )
        setRowHeaderView( rowHeaderView )
        columnHeaderView.add( timelineAxis )
        setColumnHeaderView( columnHeaderView )
// XXX TODO
//        timelineAxis.viewPort = Some( getColumnHeader )

		timelinePanel.trackList = this

        addListener( trackListListener ) // before calling addAllTracks obviously
        addAllTracks()
        followTracks()
	}

//	override def dispose {
//	}

    private val trackToolsListener: Model.Listener = {
        case TrackTools.ToolChanged( oldTool, newTool ) => {
            timelinePanel.setCursor( newTool.defaultCursor )
        }
    }

	def registerTools( tt: TrackTools ): Unit = {
        trackTools.foreach( tt => {
            tt.removeListener( trackToolsListener )
        })
        trackTools = Some( tt )
        tt.addListener( trackToolsListener )

        // update state
        trackToolsListener( TrackTools.ToolChanged( tt.currentTool, tt.currentTool ))

        // propagate
        foreach( _.renderer.trackComponent match {
            case ttl: TrackToolsListener => ttl.registerTools( tt )
            case _ =>
        })
    }

    private def addTrack( idx: Int, elem: TrackListElement ): Unit = {
        trackTools.foreach( tt => {
            elem.renderer.trackComponent match {
                case ttl: TrackToolsListener => ttl.registerTools( tt )
                case _ =>
            }
        })
        rowHeaderView.add( elem.renderer.trackHeaderComponent, idx )
        timelinePanel.add( elem.renderer.trackComponent, idx )
        rowHeaderView.revalidate()
        timelinePanel.revalidate()
    }

    private def removeTrack( idx: Int, elem: TrackListElement ): Unit = {
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
}
