/*
 *  SuperColliderPlayer.scala
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

package de.sciss.kontur
package sc

import session.Session
import de.sciss.util.Disposable
import de.sciss.synth.Server
import util.Model

class SuperColliderPlayer( client: SuperColliderClient, val doc: Session )
extends Disposable {

//    private var server: Option[ Server ] = client.server
    private var online: Option[ SCSession ] = None

    private val clientListener: Model.Listener = {
       case SuperColliderClient.ServerRunning( _ ) => serverRunning()
       case Server.Running => serverRunning()
       case Server.Offline => serverOffline()
    }

    // ---- constructor ----
    {
       val c = client.serverCondition
       if( clientListener.isDefinedAt( c )) clientListener( c )
//println( "ADDING PLAYER " + hashCode + " : condition = " + c + " -> " + clientListener.isDefinedAt( c ) + " // " + c.getClass.getName )
       client.addListener( clientListener )
    }

   override def toString = "SuperColliderPlayer(" + doc.name.getOrElse( "<Untitled>" ) + ")"

    private def serverRunning(): Unit = {
      client.server.foreach( s => {
//println( "---- CONSTRUCTING RealtimeSynthContext" )
         val context = new RealtimeSynthContext( s )
//println( "---- PERFORMING RealtimeSynthContext" )
         context.perform {
//println( "---- >>>>>> PERFORMING" )
            online = Some( new SCSession( doc ))
//println( "---- <<<<<< PERFORMING" )
         }
      })
    }

   def session: Option[ SCSession ] = online

    private def serverOffline(): Unit = {
       online.foreach( ol => {
          ol.context.consume {
             ol.dispose()
          }
          online = None
       })
    }

    def dispose(): Unit = {
       client.removeListener( clientListener )
       online.foreach( ol => {
          ol.context.perform {
             ol.dispose()
         }
         online = None
      })
    }

    def context: Option[ SynthContext ] = online.map( _.context )
}