/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.sc

import de.sciss.tint.sc.{ Server }
import de.sciss.scalaosc.{ OSCMessage }
import scala.collection.mutable.{ ListBuffer }

class MixedBundle {
   private val prepareMsgs  = new ListBuffer[ OSCMessage ]()
   private val msgs         = new ListBuffer[ OSCMessage ]()

   def send( s: Server, time: Double ) {
     
   }

   def add( m: OSCMessage ) {
     msgs += m
   }

   def addPrepare( m: OSCMessage ) {
     prepareMsgs += m
   }
}