/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.sc

import de.sciss.synth.{ Server }
import de.sciss.osc.{ OSCMessage }
import scala.collection.mutable.{ ListBuffer }

//class MixedBundle {
//   private val prepareMsgs  = new ListBuffer[ OSCMessage ]()
//   private val msgs         = new ListBuffer[ OSCMessage ]()
//
//   def send( s: Server, time: Double ) {
//      if( prepareMsgs.nonEmpty ) {
//         // XXX cheesy assumption that this is faster than the scheduled one
//         s.sendBundle( -1, prepareMsgs: _* )
//      }
//      if( msgs.nonEmpty ) {
//         s.sendBundle( time, msgs: _* )
//      }
//   }
//
//   def add( m: OSCMessage ) {
//     msgs += m
//   }
//
//   def addPrepare( m: OSCMessage ) {
//     prepareMsgs += m
//   }
//}