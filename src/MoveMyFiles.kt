import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


val destDir:String = "/home/scc/src/mine/all-repeats-attempts/Repeats-merged/app/src/main/"
//val workingDir:File = File("/home/scc/src/elucid-archival/AndroidApp")
val workingDir:File = File("/home/scc/src/mine/all-repeats-attempts/premerge")
val FILE_EXTENSIONS = arrayOf("kt", "java", "xml", "png")
fun main() {
    workingDir.walk().filter {it.isValid() && it.extension in FILE_EXTENSIONS}.forEach { movable ->
        val eftar = movable.absolutePath.after("src/main/")
        eftar?.let {
            println("$movable : $it")
            val dest:File = File(destDir+eftar)
            try {
                println("to "+destDir+eftar)
                movable.copyTo(dest, overwrite = false)
            }catch(e:FileAlreadyExistsException) {
                println("FILE ALREADY EXISTS!")

                //check if this file is identical to the existing one
                if(areIdentical(movable, dest)) {
                    println("new file is identical to existing file; skipping...")
                }else {
                    var renameIndex = 1
                    while (true) {
                        val newDest = File(destDir + eftar.substring(0,eftar.lastIndexOf(".")) + renameIndex + "." + movable.extension)
                        try {
                            println("copying to " + newDest.absolutePath)
                            movable.copyTo(newDest, overwrite = false)
                            break
                        } catch (e: FileAlreadyExistsException) {//the renamed one also already exists
                            //also check it's not identical to any of the already-renamed copies
                            if(areIdentical(movable, newDest)) {
                                println("new file is identical to ${newDest.absolutePath}, skipping...")
                                break
                            }
                            renameIndex++
                            continue
                        }
                    }
                }
            }
        }
    }

}
fun File.isValid():Boolean = isFile && canRead() && exists()

/**Return the substring of everything after the last occurrence of the argument, or null*/
fun String.after(string:String):String? {
    val index = lastIndexOf(string)
    return if(index >= 0) substring(index+string.length) else null
}

/**Checks whether specified files have identical contents.
 * Quite efficient: uses memory-mapping*/
@Throws(IOException::class)
fun areIdentical(file1:File, file2:File):Boolean {
    if(!file1.isValid()) {
        throw IOException("\'$file1\' is an invalid file")
    }
    if(!file2.isValid()) {
        throw IOException("\'$file2\' is an invalid file")
    }
    val MAX_ONE_PASS_SIZE = 50 * 1024 * 1024//the max filesize for which we can map the whole file into memory
    val ch1:FileChannel = RandomAccessFile(file1, "r").channel
    val ch2:FileChannel = RandomAccessFile(file2, "r").channel
    if (ch1.size() != ch2.size()) {
        return false//files are different sizes, and therefore not identical
    }
    val size = ch1.size()
    if(size > MAX_ONE_PASS_SIZE){//files are too big to mapped into memory in one go
        var remaining = size
        while(remaining > 0) {
            val m1: ByteBuffer = ch1.map(FileChannel.MapMode.READ_ONLY, size-remaining, MAX_ONE_PASS_SIZE.toLong())
            val m2: ByteBuffer = ch2.map(FileChannel.MapMode.READ_ONLY, size-remaining, MAX_ONE_PASS_SIZE.toLong())
            for (pos:Int in 0 until MAX_ONE_PASS_SIZE) {
                if (m1.get(pos) != m2.get(pos)) {
                    return false
                }
            }
            remaining -= MAX_ONE_PASS_SIZE
        }
        return true
    }else {//files are <20MB; map the whole of them
        val sizeInt = size.toInt()
        val m1: ByteBuffer = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size)
        val m2: ByteBuffer = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size)
        for (pos:Int in 0 until sizeInt) {
            if (m1.get(pos) != m2.get(pos)) {
                return false
            }
        }
        return true
    }
}