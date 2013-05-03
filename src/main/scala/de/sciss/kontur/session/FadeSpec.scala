/*
 *  FadeSpec.scala
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

package de.sciss.kontur.session

import scala.xml.{ Node, Null }
import de.sciss.synth._

object FadeSpec {
  def fromXML(node: Node): FadeSpec = {
    val numFrames = (node \ "numFrames").text.toLong
    val shape = (node \ "shape").headOption.map(n => {
      (n \ "@num").text.toInt match {
        case 0 => stepShape
        case 1 => linShape
        case 2 => expShape
        case 3 => sinShape
        case 4 => welchShape
        case 5 => curveShape((n \ "@curve").text.toFloat)
        case 6 => sqrShape
        case 7 => cubShape
      }
    }) getOrElse linShape
    val floor = (node \ "floor").headOption.map(_.text.toFloat) getOrElse 0f
    FadeSpec(numFrames, shape, floor)
  }
}

case class FadeSpec(numFrames: Long, shape: Env.ConstShape = linShape, floor: Float = 0f) {
  def toXML = <fade>
  <numFrames>{numFrames}</numFrames>
  {if( shape != linShape )
     <shape num={shape.id.toString} curve={shape.curvature.toString}/>
   else Null}
  {if( floor != 0f) <floor>{floor}</floor> else Null}
</fade>
}