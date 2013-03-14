package de.sciss.kontur
package desktop

import impl.BasicParamField
import java.awt.{RenderingHints, Component, Graphics, Graphics2D, Color, BasicStroke}
import javax.swing.Icon
import legacy.ParamSpace
import annotation.switch

object DefaultUnitViewFactory extends BasicParamField.UnitViewFactory {
	def createView(unit: Int): Any = {
		((unit & ParamSpace.SPECIAL_MASK): @switch) match {
      case ParamSpace.BARSBEATS => return ""
      case ParamSpace.HHMMSS    => return ClockIcon
      case ParamSpace.MIDINOTE  => return "\u266A"
      case _ =>
		}

    ((unit & ParamSpace.SCALE_MASK): @switch) match {
      case ParamSpace.PERCENT =>
        return if ((unit & ParamSpace.REL_MASK) == ParamSpace.REL) "%" else "\u0394 %"
      case ParamSpace.DECIBEL => return "dB"
      case _ =>
    }

    if ((unit & ParamSpace.REL_MASK) == ParamSpace.REL) return ""

    var unitStrShort: String = null
    val unitStr0 = ((unit & ParamSpace.UNIT_MASK): @switch) match {
      case ParamSpace.NONE    => ""
      case ParamSpace.SECS    =>
        unitStrShort = "s"
        "secs"
      case ParamSpace.SMPS    => "smps"
      case ParamSpace.BEATS   =>
        unitStrShort = "b"
        "beats"
      case ParamSpace.HERTZ   => "Hz"
      case ParamSpace.PITCH   => "pch"
      case ParamSpace.DEGREES => "\u00B0"
      case ParamSpace.METERS  => "m"
      case ParamSpace.PIXELS  => "px"
      case _                  => "???"
    }

    if (unitStrShort == null) unitStrShort = unitStr0

    val unitStr = ((unit & ParamSpace.SCALE_MASK): @switch) match {
		  case ParamSpace.MILLI => "m" + unitStrShort
  		case ParamSpace.CENTI => "c" + unitStrShort
  		case ParamSpace.KILO  => "k" + unitStrShort
      case _ => unitStr0
		}

		if ((unit & ParamSpace.REL_MASK) == ParamSpace.OFF) "\u0394 " + unitStr else unitStr
	}

	private object ClockIcon extends Icon {
		private final val strkOutline	= new BasicStroke( 1.5f )
		private final val strkZeiger	= new BasicStroke( 0.5f )
		private final val colrOutline	= new Color( 0, 0, 0, 0xC0 )

		def getIconWidth  = 16
		def getIconHeight = 16

    def paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
      val g2        = g.asInstanceOf[Graphics2D]
      val strkOrig  = g2.getStroke
      val atOrig    = g2.getTransform

      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.translate(0.5f + x, 0.5f + y) // tricky disco to blur the outlines 'bit more
      g2.setColor(colrOutline)
      g2.setStroke(strkOutline)
      g2.drawOval(x, y, 14, 14)

      g2.setStroke(strkZeiger)
      g2.setColor(Color.black)
      g2.drawLine(x + 7, y + 7, x + 7, y + 2)
      g2.drawLine(x + 7, y + 7, x + 10, y + 10)

      g2.setTransform(atOrig)
      g2.setStroke(strkOrig)
    }
	}
}
