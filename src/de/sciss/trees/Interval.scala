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
