/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.io.{ Span }
import de.sciss.trees.{ LongManager, ManagedLong, Rect, RTree }

/**
 *  Basic trail structure using R-Tree
 *  in the background
 */
class BasicTrail[ T <: Stake ] extends Trail[ T ] {
    private val tree = new RTree[ Long, Rect[ Long ]]( 1 )(
      u => new ManagedLong( u ), LongManager )

    def visitRange( span: Span, byStart: Boolean = true )( f: (T) => Unit ) = {

    }

    def visitAll( byStart: Boolean = true )( f: (T) => Unit ) = {

    }

    def get( idx: Int, byStart: Boolean = true ) : T = {
      throw new RuntimeException() // XXX
    }

	def indexOf( stake: T, byStart: Boolean = true ) : Int = {
      -1 // XXX
    }

	def indexOfPos( pos: Long, byStart: Boolean = true ) : Int = {
      -1 // XXX
    }

    def dispose {}
}
