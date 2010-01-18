/*
 *  TransportPanel.scala
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

import java.awt.{ Insets }
import javax.swing.{ AbstractButton, BoxLayout, ImageIcon, JButton, JPanel, JToggleButton }

class TransportPanel extends JPanel {
  // ---- constructor ----
  {
      val clz     = classOf[ TransportPanel ]
      val icnBeg  = new ImageIcon( clz.getResource( "transp_beg_20.png" ))
      val icnStop = new ImageIcon( clz.getResource( "transp_stop_20.png" ))
      val icnPlay = new ImageIcon( clz.getResource( "transp_play_20.png" ))
      val icnEnd  = new ImageIcon( clz.getResource( "transp_end_20.png" ))

      val butBeg  = new JButton( icnBeg )
      val butStop = new JButton( icnStop )
      val butPlay = new JButton( icnPlay )
      val butEnd  = new JButton( icnEnd )

      makeSegmented( butBeg, butStop, butPlay, butEnd )

      setLayout( new BoxLayout( this, BoxLayout.X_AXIS ))
  }

  private def makeSegmented( buttons: AbstractButton* ) {
     val first = buttons.head
     val last  = buttons.last
     buttons.foreach( b => {
/*
        b.putClientProperty( "JButton.buttonType", "bevel" )
        b.putClientProperty( "JComponent.sizeVariant", "small" )
*/
        b.putClientProperty( "JButton.buttonType", "segmentedCapsule" )
        b.putClientProperty( "JButton.segmentPosition",
          if( b == first && b == last ) "only"
          else if( b == first ) "first"
          else if( b == last ) "last"
          else "middle" )
        b.setFocusable( false )
        add( b )
     })
  }
}
