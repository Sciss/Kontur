package de.sciss.kontur
package gui

import javax.swing.{JLabel, JSlider, SwingConstants}
import java.awt.event.{MouseEvent, MouseAdapter}
import javax.swing.event.{ChangeListener, ChangeEvent}
import de.sciss.synth

class VolumeFader extends JSlider( SwingConstants.VERTICAL, -72, 18, 0 ) {
	protected var isZero = true

   // ---- constructor ----
   {
      putClientProperty( "JSlider.isFilled", java.lang.Boolean.TRUE ) // used by Metal-lnf
      setMinorTickSpacing( 3 )
      setMajorTickSpacing( 12 )
//      val fnt = AbstractApplication.getApplication.getGraphicsHandler.getFont(
//         GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI )

      val dictVolume = createStandardLabels( 12 )
      var lbZeroTmp: JLabel = null
      val en = dictVolume.elements(); while( en.hasMoreElements ) {
         val lb = en.nextElement().asInstanceOf[ JLabel ]
         lb.getText match {
            case "-72" => lb.setText( "-\u221E" )
            case "0"   =>
               lbZeroTmp = lb
               lb.setText( "0\u25C0" )
            case _ =>
         }
//         lb.setFont( fnt )
      }

      val lbZero = lbZeroTmp
      setLabelTable( dictVolume )
      setPaintTicks( true )
      setPaintLabels( true )
      setValue( 0 )

      addMouseListener( new MouseAdapter {
         override def mouseClicked( e: MouseEvent ) {
            if( e.isAltDown ) resetVolume()
         }
      })
      if( lbZero != null ) addChangeListener( new ChangeListener {
         def stateChanged( e: ChangeEvent ) {
            if( isZero ) {
               if( getValue != 0 ) {
                  isZero = false
                  lbZero.setText( "0" )
                  repaint()
               }
            } else {
               if( getValue == 0 ) {
                  isZero = true
                  lbZero.setText( "0\u25C0" )
                  repaint()
               }
            }
         }
      })
   }

	def resetVolume() {
		setValue( 0 )
	}

	def volumeDecibels : Float = {
		val db = getValue
		if( db == -72 ) Float.NegativeInfinity else db
	}

	def volumeLinear : Float = {
      import synth._
		val db = getValue
		if( db == -72 ) 0f else db.dbamp
	}

	def volumeDecibels_=( db: Float ) {
		setValue( math.max( -72, (db + 0.5f).toInt ))
	}

	def volumeLinear_=( linear: Float ) {
      import synth._
		val db = if( linear == 0f ) -72 else math.max( -72, math.min( 18, (linear.ampdb + 0.5).toInt ))
		setValue( db )
	}
}