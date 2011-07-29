/*
 *  TrackToolsPanel.scala
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

import java.awt.event.{ ActionEvent, ActionListener, KeyEvent }
import javax.swing.{ AbstractAction, Box, BoxLayout, JComboBox, JComponent, JPanel, JSlider, KeyStroke }
import javax.swing.event.{ ChangeEvent, ChangeListener }
import de.sciss.common.{ BasicMenuFactory }

object TrackToolsPanel {
   private def linexp( x: Double, inLo: Double, inHi: Double, outLo: Double, outHi: Double ) =
      math.pow( outHi / outLo, (x - inLo) / (inHi - inLo) ) * outLo
}

class TrackToolsPanel( trackList: TrackList, timelineView: TimelineView )
extends JPanel with TrackTools {
   import TrackTools._
   import TrackToolsPanel._

   private val tools = List( new TrackCursorTool( trackList, timelineView ),
                             new TrackMoveTool( trackList, timelineView ),
                             new TrackResizeTool( trackList, timelineView ),
                             new TrackGainTool( trackList, timelineView ),
                             new TrackFadeTool( trackList, timelineView ),
                             new TrackSlideTool( trackList, timelineView ),
                             new TrackMuteTool( trackList, timelineView )
   )

    private var currentToolVar: TrackTool = tools.head
    private val ggCombo = new JComboBox()
    private var visualBoostVar = 1f;

    // ---- constructor ----
    {
        val imap	= ggCombo.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
        val amap    = ggCombo.getActionMap
        val meta    = BasicMenuFactory.MENU_SHORTCUT

        var i = 1; tools.foreach( t => {
            val key = "tool" + i
            imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_0 + i, meta ), key )
            val action = new ToolAction( t )
            amap.put( key, action )
            ggCombo.addItem( action )
            i += 1
        })

        ggCombo.addActionListener( new ActionListener {
            def actionPerformed( e: ActionEvent ) {
                ggCombo.getSelectedItem match {
                    case t: ToolAction => t.perform()
                    case _ =>
                }
            }
        })
        ggCombo.setFocusable( false )
        ggCombo.putClientProperty( "JComboBox.isSquare", java.lang.Boolean.TRUE )
        ggCombo.putClientProperty( "JComponent.sizeVariant", "small" )

        val ggVisualBoost = new JSlider( 0, 128 )
        ggVisualBoost.setFocusable( false )
        ggVisualBoost.putClientProperty( "JComponent.sizeVariant", "small" )
        ggVisualBoost.addChangeListener( new ChangeListener {
           def stateChanged( e: ChangeEvent ) {
              val newVisualBoost = linexp( ggVisualBoost.getValue, 0, 128, 1, 512 ).toFloat
              if( newVisualBoost != visualBoostVar ) {
                 val change = VisualBoostChanged( visualBoostVar, newVisualBoost )
                 visualBoostVar = newVisualBoost
                 dispatch( change )
              }
           }
        })

        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ))
        add( Box.createHorizontalGlue )
        add( ggCombo )
        add( ggVisualBoost )
        add( Box.createHorizontalGlue )
    }

    private def changeTool( newTool: TrackTool ) {
        if( newTool != currentToolVar ) {
            val change = ToolChanged( currentToolVar, newTool )
            currentToolVar = newTool
            dispatch( change )
        }
    }

    def currentTool: TrackTool = currentToolVar
    def visualBoost: Float = visualBoostVar

    private class ToolAction( t: TrackTool ) extends AbstractAction( t.name ) {
        def actionPerformed( e: ActionEvent ) {
            ggCombo.setSelectedItem( this )
        }

        def perform() { changeTool( t )}

        override def toString = t.name
    }
}