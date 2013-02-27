package de.sciss.kontur.util

import collection.immutable.Queue
import util.control.NonFatal

object Model {
  type Listener = PartialFunction[Any, Unit]
}
trait Model {
  import Model._

  private var listeners = Queue.empty[Listener]
  private val sync = new AnyRef

  protected def dispatch(change: Any) {
    listeners foreach { l =>
      try {
        if (l isDefinedAt change) l(change)
      } catch {
        case NonFatal(e) => e.printStackTrace() // catch, but print
      }
    }
  }

  def addListener(l: Listener): Listener = {
    sync.synchronized {
      listeners = listeners.enqueue(l)
    }
    l
  }

  def removeListener(l: Listener): Listener = {
    sync.synchronized {
      // multi set diff just removes one instance --
      // observers could register more than once if they want
      listeners = listeners.diff(List(l))
    }
    l
  }
}