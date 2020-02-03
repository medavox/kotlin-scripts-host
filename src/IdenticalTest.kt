import java.io.File

fun main(args:Array<String>) {
    if(args.size != 2) {
        usage()
    }
    val a = File(args[0])
    val b = File(args[1])
    if(!a.isValid() || !b.isValid()) {
        usage()
    }
    println(if(areIdentical(a, b)) "files are identical" else "files are different")
}

private fun usage() {
    System.err.println("invalid arguments.")
    println("Usage:")
    println("programname <file1> <file2>")
    System.exit(1)
}