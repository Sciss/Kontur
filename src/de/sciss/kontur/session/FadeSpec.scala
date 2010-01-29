/*
 *  FadeSpec.scala
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

package de.sciss.kontur.session

import scala.xml.{ Node }
import de.sciss.tint.sc.{ EnvShape, linearShape, varShape, SC }
import SC._

object FadeSpec {
   def fromXML( node: Node ) : FadeSpec = {
      val numFrames  = (node \ "numFrames").text.toLong
      val shape      = (node \ "shape").headOption.map( n => {
         Tuple2( (n \ "@num").text.toInt, (n \ "@curve").text.toFloat )
      }) getOrElse (1, 0f)
      val floor      = (node \ "floor").headOption.map( _.text.toFloat ) getOrElse 0f
      FadeSpec( numFrames, shape, floor )
   }
}

case class FadeSpec( numFrames: Long, shape: Tuple2[ Int, Float ] = (1,0f), floor: Float = 0f ) {
  def toXML = <fade>
  <numFrames>{numFrames}</numFrames>
  {if( shape != linearShape )
     Some( <shape num={shape._1.toString} curve={shape._2.toString}/> )
   else None}
  {if( floor != 0f) Some( <floor>{floor}</floor> ) else None}
</fade>
}