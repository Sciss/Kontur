/*
 *  Track.scala
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

import java.awt.datatransfer.DataFlavor
import java.io.IOException
import scala.xml.Node
import de.sciss.kontur.edit.Editor
import de.sciss.kontur.util.SerializerContext
import de.sciss.span.Span
import legacy.AbstractCompoundEdit

object Track {
   val flavor = new DataFlavor( classOf[ Track ], "Track" )
}

trait Track extends SessionElement {
   type T <: Stake[ T ]
   def trail: Trail[ T ]
   def editor: Option[ TrackEditor ]
}

trait TrackEditor extends Editor {
   def editRename( ce: AbstractCompoundEdit, newName: String ) : Unit
}

class Tracks( doc: Session )
extends BasicSessionElementSeq[ Track ]( doc, "Tracks" ) {

   def toXML( c: SerializerContext ) =
      <tracks id={c.id( this ).toString}>
      {innerToXML( c )}
      </tracks>

   def fromXML( c: SerializerContext, parent: Node ): Unit = {
      val innerXML = SessionElement.getSingleXML( parent, "tracks" )
      innerFromXML( c, innerXML )
   }

   protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ Track ] =
      node.child.filter( _.label != "#PCDATA" ).map( ch => {
         (ch \ "@idref").headOption.map( attr => {
            c.byID[ Track ]( attr.text.toLong )
         }).getOrElse( ch.label match {
            case AudioTrack.XML_NODE => AudioTrack.fromXML( c, ch, doc )
            case lb => throw new IOException( "Unknown track type '" + lb + "'" )
         })
      })

   def getStakes( span: Span, overlap: Boolean = true )( filter: (Stake[ _ ]) => Boolean ) : List[ Stake[ _ ]] = {
      var result: List[ Stake[ _ ]] = Nil
      foreach( t => {
         t.trail.visitRange( span )( stake => {
            if( (overlap || span.contains( stake.span )) && filter( stake )) result ::= stake
         })
      })
      result
   }
}