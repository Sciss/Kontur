/*
 *  Shape.scala
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

package de.sciss.trees

trait Shaped[ U ] { def shape: Shape[ U ]}

trait Shape[ U ] extends Shaped[ U ]{
	// Shaped trait
	def shape = this

	def enlargement( s2: Shape[ U ]) : U
	def area : U
	def union( s2: Shape[ U ]) : Shape[ U ]
	def dim : Int
	def interval( dim: Int ) : Interval[ U ]
    def overlaps( s2: Shape[ U ]) : Boolean
//	def intervals : scala.collection.immutable.Vector[ Interval[ U ]]

    def low( dim: Int ) : U = interval( dim ).low
	def high( dim: Int ) : U = interval( dim ).high
	def span( dim: Int ) : U = interval( dim ).span
}
