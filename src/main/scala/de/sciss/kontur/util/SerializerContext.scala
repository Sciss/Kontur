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