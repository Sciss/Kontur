/*
 *  SessionElement.scala
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

package de.sciss.kontur
package session

import util.{Model, SerializerContext}
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
