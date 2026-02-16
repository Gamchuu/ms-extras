package gamchu.pathfinder

class BinaryHeapOpenSet(initialCapacity: Int = 1024) {
    private var heap: Array<PathNode?> = arrayOfNulls(initialCapacity)
    var size: Int = 0
        private set

    fun isEmpty(): Boolean = size == 0

    fun insert(node: PathNode) {
        if (size == heap.size) grow()
        heap[size] = node
        node.heapIndex = size
        siftUp(size)
        size++
    }

    fun poll(): PathNode {
        val min = heap[0]!!
        size--
        if (size > 0) {
            heap[0] = heap[size]
            heap[0]!!.heapIndex = 0
            heap[size] = null
            siftDown(0)
        } else {
            heap[0] = null
        }
        min.heapIndex = -1
        return min
    }

    fun decreaseKey(node: PathNode) {
        siftUp(node.heapIndex)
    }

    private fun siftUp(startIndex: Int) {
        var i = startIndex
        val node = heap[i]!!
        while (i > 0) {
            val parentIdx = (i - 1) ushr 1
            val parent = heap[parentIdx]!!
            if (node.fCost >= parent.fCost) break
            heap[i] = parent
            parent.heapIndex = i
            i = parentIdx
        }
        heap[i] = node
        node.heapIndex = i
    }

    private fun siftDown(startIndex: Int) {
        var i = startIndex
        val node = heap[i]!!
        val half = size ushr 1
        while (i < half) {
            var child = (i shl 1) + 1
            var childNode = heap[child]!!
            val right = child + 1
            if (right < size) {
                val rightNode = heap[right]!!
                if (rightNode.fCost < childNode.fCost) {
                    child = right
                    childNode = rightNode
                }
            }
            if (node.fCost <= childNode.fCost) break
            heap[i] = childNode
            childNode.heapIndex = i
            i = child
        }
        heap[i] = node
        node.heapIndex = i
    }

    private fun grow() {
        val newCapacity = heap.size * 2
        heap = heap.copyOf(newCapacity)
    }
}
