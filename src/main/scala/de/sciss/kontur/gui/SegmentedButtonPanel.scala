/*
 *  SegmentedButtonPanel.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur.gui

import javax.swing.{ AbstractButton, BoxLayout, JPanel }

class SegmentedButtonPanel extends JPanel {

  // ---- constructor ----
  {
      setLayout( new BoxLayout( this, BoxLayout.X_AXIS ))
  }

  protected def setButtons( buttons: List[ AbstractButton ], style: String = "capsule" ): Unit = {
      removeAll()
      makeSegmented( buttons, style )
      buttons.foreach( b => add( b ))
  }

  private def makeSegmented( buttons: List[ AbstractButton ], style: String ): Unit = {
     if( buttons.isEmpty ) return
     val first = buttons.head
     val last  = buttons.last
     val segmStyle = "segmented" + style.capitalize
     buttons.foreach( b => {
        b.putClientProperty( "JButton.buttonType", segmStyle )
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
