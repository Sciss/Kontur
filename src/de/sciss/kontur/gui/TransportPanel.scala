/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
