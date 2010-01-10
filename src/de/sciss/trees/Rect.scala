package de.sciss.trees

import _root_.scala.collection.immutable.Vector

case class Rect[ U ]( intervals: Vector[ Interval[ U ]])( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ])
extends Shape[ U ] {

	import mgr._

	val dim : Int = intervals.size

	lazy val area : U = {
		var i = 0
		var a = one
		while( i < dim ) {
			a *= interval( i ).span
			i += 1
		}
		a
	}

	def enlargement( s2: Shape[ U ]) : U = {
		union( s2 ).area - area
	}

	def union( s2: Shape[ U ]) : Shape[ U ] = {
		if( s2.dim != dim ) throw new IllegalArgumentException( "Dimensions do not match (" + dim + " vs. " + s2.dim + ")" )

		val v2 = new Array[ Interval[ U ]]( dim )
	    var i = 0
	    while( i < dim ) {
	    	v2( i ) = interval( i ).union( s2.interval( i ))
	    	i += 1
	    }
		Rect( Vector( v2:_* ))
	}

	def overlaps( s2: Shape[ U ]) : Boolean = {
		if( s2.dim != dim ) throw new IllegalArgumentException( "Dimensions do not match (" + dim + " vs. " + s2.dim + ")" )

		var i = 0
		while( i < dim ) {
			if( !interval( i ).overlaps( s2.interval( i ))) return false
			i += 1
		}
		true
	}

	def interval( dim: Int ) : Interval[ U ] = intervals( dim )
}
