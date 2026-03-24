package cz.cuni.gamedev.nail123.mcworldgeneration.visualizer

class List2D<T> (val width: Int, val height: Int, initializer: (Int) -> T) {
    val data = MutableList(width * height, initializer)

    operator fun get(x: Int, y: Int) = data[width * y + x]
    operator fun set(x: Int, y: Int, value: T) {
        data[width * y + x] = value
    }
}

class IntArray2D (val width: Int, val height: Int, initializer: (Int) -> Int) {
    val data = IntArray(width * height, initializer)

    operator fun get(x: Int, y: Int) = data[width * y + x]
    operator fun set(x: Int, y: Int, value: Int) {
        data[width * y + x] = value
    }
}
