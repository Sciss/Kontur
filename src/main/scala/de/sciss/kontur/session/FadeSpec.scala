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

/**
 *    @version 0.11, 23-May-10
 */
object FadeSpec {
   def fromXML( node: Node ) : FadeSpec = {
      val numFrames  = (node \ "numFrames").text.toLong
     import Curve._
      val shape      = (node \ "shape").headOption.map( n => {
         (n \ "@num").text.toInt match {
            case 0 => step
            case 1 => linear
            case 2 => exp
            case 3 => sine
            case 4 => welch
            case 5 => parametric( (n \ "@curve").text.toFloat )
            case 6 => squared
            case 7 => cubed
         }
      }) getOrElse linear
      val floor      = (node \ "floor").headOption.map( _.text.toFloat ) getOrElse 0f
      FadeSpec( numFrames, shape, floor )
   }
}

case class FadeSpec( numFrames: Long, shape: Curve = Curve.linear, floor: Float = 0f ) {
  def toXML = <fade>
  <numFrames>{numFrames}</numFrames>
  {if( shape != Curve.linear )
     <shape num={shape.id.toString} curve={(shape match { case Curve.parametric(c) => c; case _ => 0f }).toString}/>
   else Null}
  {if( floor != 0f) <floor>{floor}</floor> else Null}
</fade>
}