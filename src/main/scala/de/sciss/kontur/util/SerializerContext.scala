/*
 *  SerializerContext.scala
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

package de.sciss.kontur.util

import scala.xml.NodeSeq

trait SerializerContext {
   def id( obj: AnyRef ) : Long
   def id( obj: AnyRef, ident: Long ) : Unit
   def getByID[ T ]( ident: Long ) : Option[ T ]
   def exists( obj: AnyRef ) : Boolean

   def id( obj: AnyRef, node: NodeSeq ) {
      val ident = (node \ "@id").text.toLong
      id( obj, ident )
   }

   def getByID[ T ]( node: NodeSeq ) : Option[ T ] =
      getByID( (node \ "@idref").text.toLong )

   def byID[ T ]( ident: Long ) : T =
      getByID( ident ).getOrElse( throw new NoSuchElementException( "Referencing an invalid element #" + ident ))

   def byID[ T ]( node: NodeSeq ) : T = byID( (node \ "@idref").text.toLong )
}

class BasicSerializerContext extends SerializerContext {
   private var mapObjToID = Map[ AnyRef, Long ]()
   private var mapIDToObj = Map[ Long, AnyRef ]()
   private var idCount = 0L

   def exists( obj: AnyRef ) = mapObjToID.contains( obj )

   private def createID : Long = {
      val res = idCount
      idCount += 1
      res
   }

   def id( obj: AnyRef ) : Long = {
      mapObjToID.get( obj ) getOrElse {
         val ident = createID
         id( obj, ident )
         ident
      }
   }

   def id( obj: AnyRef, ident: Long ) {
      mapObjToID += obj -> ident
      mapIDToObj += ident -> obj
   }

   def getByID[ T ]( ident: Long ) = mapIDToObj.get( ident ).asInstanceOf[ Option[ T ]]
}