/*
 *  SuperColliderPlayer.scala
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
package sc

import session.Session
import de.sciss.synth.Server
import util.Model

class SuperColliderPlayer(client: SuperColliderClient, val doc: Session) {
  private var online: Option[SCSession] = None

  private val clientListener: Model.Listener = {
    case SuperColliderClient.ServerRunning(_) => serverRunning()
    case Server.Running                       => serverRunning()
    case Server.Offline                       => serverOffline()
  }

  // ---- constructor ----
  {
    val c = client.serverCondition
    if (clientListener.isDefinedAt(c)) clientListener(c)
    //println( "ADDING PLAYER " + hashCode + " : condition = " + c + " -> " + clientListener.isDefinedAt( c ) + " // " + c.getClass.getName )
    client.addListener(clientListener)
  }

  override def toString = "SuperColliderPlayer(" + doc.name.getOrElse("<Untitled>") + ")"

  private def serverRunning(): Unit = {
    client.server.foreach { s =>
      //println( "---- CONSTRUCTING RealtimeSynthContext" )
      val context = new RealtimeSynthContext(s)
      //println( "---- PERFORMING RealtimeSynthContext" )
      context.perform {
        //println( "---- >>>>>> PERFORMING" )
        online = Some(new SCSession(doc))
        //println( "---- <<<<<< PERFORMING" )
      }
    }
  }

  def session: Option[SCSession] = online

  private def serverOffline(): Unit = {
    online.foreach { ol =>
      ol.context.consume {
        ol.dispose()
      }
      online = None
    }
  }

  def dispose(): Unit = {
    client.removeListener(clientListener)
    online.foreach { ol =>
      ol.context.perform {
        ol.dispose()
      }
      online = None
    }
  }

  def context: Option[SynthContext] = online.map(_.context)
}