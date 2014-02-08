/*
 *  Interval.scala
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

case class Interval[ U ]( low: U, high: U )( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ]) {
//	lazy val span = high - low

	def span = high - low

	def union( i: Interval[ U ]) : Interval[ U ] = {
		Interval( low.min( i.low ), high.max( i.high ))
	}

	def overlaps( i: Interval[ U ]) : Boolean = {
		(i.low < high) && (i.high > low)
	}
}
