/**
 * 	RTree
 * 	(de.sciss.tree package)
 *
 *  Copyright (c) 2009-2013 Hanns Holger Rutz. All rights reserved.
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
package de.sciss.trees

import annotation.tailrec
import language.existentials

//import Version._

/**
 * 	Note: to make persistent, the
 * 	following fields have to be enhanced:
 *
 *	Tree
 *		root		(FP)	OK
 *
 *	Node
 *		b			(FP)	OK
 *		numChildren	(FV)	OK
 *		children	(FP)	OK???
 *
 *	@version	0.13, 04-Jan-10
 *	@author		Hanns Holger Rutz
 */
class RTree[ U, V <: Shaped[ U ]]( val dim: Int, val capacity: Int = 10, val minFill: Int = 3 /* Math.min( capacity / 2, 3 ) */)( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ]) {
	type Node  = RTreeNode[ U, V, _ ]
	type Index = RTreeIndex[ U, V ]
    type Leaf  = RTreeLeaf[ U, V ]

//	private val root = new FatPointer[ Node ]()
	private var root : RTreeNode[ U, V, _ ] = null


	// constructor
	clear()

	def clear() {
//		root.set( new Leaf( this, Nil )( view, mgr ))
		root = new Leaf( this, Nil )( view, mgr )
	}

   def isEmpty : Boolean = root.numChildren == 0

	def insert( v: V ) {
//		val rootID = root.get()
//		val rootG = if( rootID == null ) {
//			root.get().value
//		} else {
//			rootID.value
//		}
//println( "INSERT : " + v + "; root is " + rootG )
	  	val (path, leaf) = root.chooseLeaf( v )
	  	if( leaf.numChildren < capacity ) {
//println( "CASE 1 : FITS" )
	  		leaf.insert( v )
	  		adjustTree( path )
	  	} else {
	  		val (leaf1, leaf2) = leaf.splitNode( v )
//	  		if( leaf == root ) { ... }
	  		if( equalsNode( leaf, root )) {
//println( "CASE 2 : NEW ROOT" )
	  			val rootIdx = new Index( this, List( leaf1, leaf2 ))( view, mgr )
//	  			root.set( rootIdx )
                root = rootIdx
	  			adjustTree( List( rootIdx ))
	  		} else {
//println( "CASE 3 : NEW SUBINDEX" )
//	  			println( "Not identical: leaf = " + leaf + "; root = " + root.get() )
	  			val p = path.head
	  			p.remove( leaf )
	  			p.insert( leaf1 )
	  			p.updateBounds()
		  		adjustTree( path, leaf2 )
	  		}
	  	}
//println( "FINALLY root is " + root.get )
	}

	private def equalsNode( a: Node, b: Node ) : Boolean = (a == b)

	def remove( v: V ) {
		val (path, leaf) = root.findLeaf( v )
		if( leaf == null ) return

		leaf.remove( v )
		leaf.updateBounds()
		condenseTree( path, leaf )
	}

	def findOverlapping( shape: Shape[ U ], visitor: (V) => Unit ) {
		root.findOverlapping( shape, visitor )
	}

	def getRoot: Node = root

	def debugDump() {
		root.debugDump( 0 )
	}

 	private def adjustTree( path: Seq[ Index ]) {
		path.foreach( _.updateBounds() )
 	}

	@tailrec
 	private def adjustTree( path: Seq[ Index ], split: Node ) {
		if( path.isEmpty ) return
 		val p = path.head
 		if( p.numChildren < capacity ) {
 			p.insert( split )
 			adjustTree( path )
 		} else {
 			val (idx1, idx2) = p.splitNode( split )
 			val tail = path.tail
 			if( tail.isEmpty ) {
	  			val rootIdx = new Index( this, List( idx1, idx2 ))( view, mgr )
//	  			root.set( rootIdx )	// XXX side effect, should be handled by caller
                root = rootIdx
	  			return
 			}
 			val pp = tail.head
 			pp.remove( p )
 			pp.insert( idx1 )
 			pp.updateBounds()
 	 		adjustTree( tail, idx2 )
 		}
 	}

	private def condenseTree( path: Seq[ Index ], last: Node ) {
		var q: List[ Node ] = Nil
		var n = last
		path.foreach( p => {
			if( n.numChildren < minFill ) {
				p.remove( n )
				p.updateBounds()
				q ::= n
			}
			if( p.numChildren >= minFill ) { // only if it won't be removed in the next iteration
				p.updateBounds()
			}
			n = p
		})

		if( (root.numChildren < minFill) && !root.isLeaf ) {
			q ::= root
			clear()
		}

		while( q.nonEmpty ) {
			var qRest: List[ Node ] = Nil
			q.foreach( p => {
				if( p.isLeaf ) {
					p.asLeaf.getChildren.foreach( insert(_) )
				} else {
					qRest :::= p.asIndex.getChildren
				}
			})
			q = qRest
		}
	}
}

protected abstract class RTreeNode[ U, V <: Shaped[ U ], C /*<: Shaped[ U ]*/]
    ( val tree: RTree[ U, V ], c: List[ C ], val isLeaf: Boolean )
	( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ])
extends Shaped[ U ] {

	import mgr._

//    val numChildren: FatValue[ Int ] = c.size // XXX
    var numChildren = c.size
//	protected val b = new FatPointer[ Shape[ U ]]()
	protected var b: Shape[ U ] = null
	protected var bValid = false

//	protected val children: FatPointer[ List[ C ]] = c
	protected var children: List[ C ] = c

	// constructor
	updateBounds()

//	def getChildren : List[ C ] = children.get.value
    def getChildren : List[ C ] = children

	def asIndex: tree.Index
    def asLeaf: tree.Leaf

    def newInstance( c: List[ C ]) : RTreeNode[ U, V, C ]

    def bounds: Shape[ U ] = {
//		if( b == null ) updateBounds	// lazy
    	if( !bValid ) throw new IllegalStateException
		b
	}

// OOO
//	def insert( n: C ) {
////		children ::= n
//    	// XXX space inefficient, maybe better
//    	// to use a fat list?
//		children.set( n :: children )
////		numChildren += 1
//		numChildren.set( numChildren + 1 )
////		if( b != null ) ...
//
//		if( bValid ) b.set( b.union( n.shape ))
//	}

	def remove( n: C ) {
		children = children.diff( List( n )) // XXX
//		children.set( children.diff( List( n ))) // XXX
		numChildren -= 1
//		numChildren.set( numChildren - 1 )
		bValid = false
	}

	def updateBounds() : Unit
// OOO
//	def updateBounds {
//		b.set( if( children.isEmpty ) {
//			Rect( Vector.fill( tree.dim )( Interval( zero, zero )))
//		} else {
//			children.tail.foldLeft( children.head.shape )( (mbr, next) => mbr.union( next.shape ))
//		})
//		bValid = true
//	}

	def shape: Shape[ U ] = bounds
    def debugDump( level: Int ) : Unit

    def chooseLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf)
   	def findLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf)

    def findOverlapping( shape: Shape[ U ], visitor: (V) => Unit ) : Unit

// OOO
// 	def splitNode( n: C ) : Tuple2[ RTreeNode[ U, V, C ], RTreeNode[ U, V, C ]] = {
//		val (g1, g2 ) = linearSplit( n :: children )
//		val n1 = newInstance( g1 )
//		val n2 = newInstance( g2 )
//		Tuple2( n1, n2 )
//    }

    @tailrec
	protected final def chooseLeaf( path: List[ tree.Index ], shape: Shape[ U ]) :
		(Seq[ tree.Index ], tree.Leaf) = {

	  	val n = path.head.children.foldLeft( Tuple2[ tree.Node, U ]( null, max ))(
	  		(best, child) => {
	  			val bestNode	= best._1
	  			val bestEnlarge	= best._2
	  			val enlarge = shape.enlargement( child.shape )
	  			if( (enlarge < bestEnlarge) || ((enlarge == bestEnlarge) && (child.bounds.area < bestNode.bounds.area)) ) {
	  				Tuple2( child, enlarge)
	  			} else best
	  		}
	  	)._1

	  	if( n.isLeaf ) return (path -> n.asLeaf)

		chooseLeaf( n.asIndex :: path, shape )	// unfortunately the only way to guarantee tail recursion
	}

	protected final def findLeaf( path: List[ tree.Index ], v: V ) :
		(Seq[ tree.Index ], tree.Leaf) = {

		path.head.children.foreach( child => {
			if( child.isLeaf ) {
				val leaf = child.asLeaf
				if( leaf.children.contains( v )) {
					return (path -> leaf)
				}
			} else {
				val result = findLeaf( child.asIndex :: path, v )
				if( result._2 != null ) return result
			}
		})

		(path -> null)
	}

	protected final def linearSplit[ A <: Shaped[ U ]]( seq: List[ A ]) :
		(List[ A ], List[ A ]) = {

		var entries = seq
		val result = pickSeeds( entries )
		val e1	= result._1
		val e2	= result._2
		entries = result._3
		var g1 = List( e1 )
		var g2 = List( e2 )
		var g1Size = 1
		var g2Size = 1
		var num = numChildren - 2
		var g1Bounds : Shape[ U ] = e1.shape
		var g2Bounds : Shape[ U ] = e2.shape

		while( !entries.isEmpty ) {
			if( g1Size + num == tree.minFill ) {
				g1 :::= entries
				return Tuple2( g1, g2 )
			} else if( g2Size + num == tree.minFill ) {
				g2 :::= entries
				return Tuple2( g1, g2 )
			}

			val e3		= entries.head
			entries		= entries.tail
			num		   -= 1

			val g1Enlarge = g1Bounds.enlargement( e3.shape )
			val g2Enlarge = g2Bounds.enlargement( e3.shape )
			if( (g1Enlarge < g2Enlarge) || ((g1Enlarge == g2Enlarge) && (g1Bounds.area < g2Bounds.area)) ) {
				g1 ::= e3
				g1Bounds = g1Bounds.union( e3.shape )
				g1Size += 1
			} else {
				g2 ::= e3
				g2Bounds = g2Bounds.union( e3.shape )
				g2Size += 1
			}
		}
		Tuple2( g1, g2 )
	}

	protected final def pickSeeds[ A <: Shaped[ U ]]( entries: List[ A ]) : (A, A, List[ A ]) = {
		var maxSeparation : U = min // zero
		var e1: A = null.asInstanceOf[ A ]
        var e2: A = null.asInstanceOf[ A ]

        for( i <- (0 until tree.dim) ) {
        	val maxLowElem  = entries.reduceLeft( (e1, e2) => if( e1.shape.low( i ) > e2.shape.low( i )) e1 else e2 )
        	val minHighElem = entries.reduceLeft( (e1, e2) => if( e1.shape.high( i ) < e2.shape.high( i )) e1 else e2 )
        	val minLow		= entries.reduceLeft( (e1, e2) => if( e1.shape.low( i ) < e2.shape.low( i )) e1 else e2 ).shape.low( i )
        	val maxHigh		= entries.reduceLeft( (e1, e2) => if( e1.shape.high( i ) > e2.shape.high( i )) e1 else e2 ).shape.high( i )
        	val width		= maxHigh - minLow
        	if( width > zero ) {
        		val sep		= (maxLowElem.shape.low( i ) - minHighElem.shape.high( i )).abs / width
        		if( sep > maxSeparation ) {
        			maxSeparation	= sep
        			e1				= maxLowElem
        			e2				= minHighElem
        		}
        	} else {
        		maxSeparation	= width
        	}
        }

		Tuple3( e1, e2, entries.diff( List( e1, e2 ))) // XXX
	}
}

/*
 * 	This corresponds to what Guttman calls "non-leaf node".
 * 	The name was chosen to be somewhat similar to the naming
 * 	in the SaIL library.
 */
protected class RTreeIndex[ U, V <: Shaped[ U ]]( t: RTree[ U, V ], c: List[ RTreeNode[ U, V, _ ]])
	( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ])
extends RTreeNode[ U, V, RTreeNode[ U, V, _ ]]( t, c, false )( view, mgr ) {

	import mgr._

	def updateBounds() {
		b = if( children.isEmpty ) {
			Rect( Vector.fill( tree.dim )( Interval( zero, zero )))
		} else {
			children.tail.foldLeft( children.head.shape )( (mbr, next) => mbr.union( next.shape ))
		}
		bValid = true
	}

 	def splitNode( n: RTreeNode[ U, V, _ ]) : (RTreeNode[ U, V, _ ], RTreeNode[ U, V, _ ]) = {
		val (g1, g2 ) = linearSplit( n :: children )
		val n1 = newInstance( g1 )
		val n2 = newInstance( g2 )
		Tuple2( n1, n2 )
    }

	def insert( n: RTreeNode[ U, V, _ ]) {
    	// XXX space inefficient, maybe better
    	// to use a fat list?
		children = n :: children
		numChildren += 1

		if( bValid ) b = b.union( n.shape )
	}

	def asIndex: tree.Index = this
	def asLeaf: tree.Leaf = throw new UnsupportedOperationException
    def newInstance( c: List[ tree.Node ]) : RTreeNode[ U, V, tree.Node ] = new tree.Index( tree, c )( view, mgr )

	def chooseLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf) =
		chooseLeaf( List( this ), v.shape )

	def findLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf) =
		findLeaf( List( this ), v )

	def findOverlapping( shape: Shape[ U ], visitor: (V) => Unit ) {
		children.foreach( _.findOverlapping( shape, visitor ))
	}

    def debugDump( level: Int ) {
		for( i <- (0 until level) ) print( "  " )
		println( "Index : " + shape )
		children.foreach( _.debugDump( level + 1 ))
	}

 	override def toString : String = "Index( " + children + " )"
}

protected class RTreeLeaf[ U, V <: Shaped[ U ]]( t: RTree[ U, V ], c: List[ V ])
	( implicit view: (U) => ManagedNumber[ U ], mgr: NumberManager[ U ])
extends RTreeNode[ U, V, V ]( t, c, true )( view, mgr ) {

	import mgr._

	def updateBounds() {
		b = if( children.isEmpty ) {
			Rect( Vector.fill( tree.dim )( Interval( zero, zero )))
		} else {
			children.tail.foldLeft( children.head.shape )( (mbr, next) => mbr.union( next.shape ))
		}
		bValid = true
	}

 	def splitNode( n: V ) : (RTreeNode[ U, V, _ ], RTreeNode[ U, V, _ ]) = {
		val (g1, g2 ) = linearSplit( n :: children )
		val n1 = newInstance( g1 )
		val n2 = newInstance( g2 )
		Tuple2( n1, n2 )
    }

 	def insert( n: V ) {
    	// XXX space inefficient, maybe better
    	// to use a fat list?
		children = n :: children
		numChildren += 1
		if( bValid ) b = b.union( n.shape )
	}

	def asIndex: tree.Index  = throw new UnsupportedOperationException
    def asLeaf: tree.Leaf = this
    def newInstance( c: List[ V ]) : RTreeNode[ U, V, V ] = new tree.Leaf( tree, c )( view, mgr )

	def chooseLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf) = (Nil -> this)
	def findLeaf( v: V ) : (Seq[ tree.Index ], tree.Leaf) =
		(Nil -> (if( children.contains( v )) this else null) )

    def findOverlapping( shape: Shape[ U ], visitor: (V) => Unit ) {
		children.foreach( child => {
			if( shape.overlaps( child.shape )) visitor( child )
		})
	}

    def debugDump( level: Int ) {
		for( i <- (0 until level) ) print( "  " )
		println( "Leaf : " + shape )
		children.foreach( child => {
			for( i <- (0 to level) ) print( "  " )
			println( child )
		})
	}

 	override def toString : String = "Leaf( " + children + " )"
}