/*
 *  FadeSpec.scala
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

package de.sciss.kontur.session

import scala.xml.{ Node, Null }
import de.sciss.synth._

object FadeSpec {
  def fromXML(node: Node): FadeSpec = {
    val numFrames = (node \ "numFrames").text.toLong
    val shape = (node \ "shape").headOption.map(n => {
      (n \ "@num").text.toInt match {
        case 0 => Curve.step
        case 1 => Curve.linear
        case 2 => Curve.exponential
        case 3 => Curve.sine
        case 4 => Curve.welch
        case 5 => Curve.parametric((n \ "@curve").text.toFloat)
        case 6 => Curve.squared
        case 7 => Curve.cubed
      }
    }) getOrElse Curve.linear
    val floor = (node \ "floor").headOption.map(_.text.toFloat) getOrElse 0f
    FadeSpec(numFrames, shape, floor)
  }
}

case class FadeSpec(numFrames: Long, shape: Curve = Curve.linear, floor: Float = 0f) {
  def toXML = <fade>
  <numFrames>{numFrames}</numFrames>
  {if( shape != Curve.linear)
     <shape num={shape.id.toString} curve={(shape match { case Curve.parametric(c) => c; case _ => 0f }).toString}/>
   else Null}
  {if( floor != 0f) <floor>{floor}</floor> else Null}
</fade>
}