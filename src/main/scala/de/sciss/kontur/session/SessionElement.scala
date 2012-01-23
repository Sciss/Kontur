/*
 *  SessionElement.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2012 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.kontur.util.SerializerContext
import de.sciss.synth.Model
import java.io.IOException
import scala.xml.Node

object SessionElement {
  @throws( classOf[ IOException ])
  def getSingleXML( parent: Node, label: String ) : Node = {
      val seq = parent \ label
      val sz  = seq.size
      if( sz == 0 ) throw new IOException( "Missing XML element '" + label + "'" )
      if( sz > 1 ) throw new IOException( "XML element '" + label + "' appears more than once" )
      seq.head
  }
}

trait SessionElement extends Model {
  def name: String
//  def id: Long
//  def toXML =
//    <elem name={name}>
//    </elem>

  def toXML( c: SerializerContext ): scala.xml.Elem

//  @throws( classOf[ IOException ])
//  def fromXML( parent: Node ) : Unit
}
